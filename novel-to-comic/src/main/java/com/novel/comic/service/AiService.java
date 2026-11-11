package com.novel.comic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novel.comic.config.AppConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for AI API calls (streaming + non-streaming).
 * Compatible with Anthropic-compatible APIs.
 */
@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AppConfig.PipelineConfig config;
    private final ObjectMapper mapper;
    private final OkHttpClient client;

    public AiService(AppConfig.PipelineConfig config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)  // 5 min for long responses
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Callback interface for streaming chunks.
     */
    public interface StreamCallback {
        void onChunk(String text);
        void onChunk(String text, int index);
        void onStatus(String message);
        void onProgress(String message, int done, int total);
    }

    /**
     * Load a prompt template file and replace variables.
     * Variables are in {{KEY}} format.
     */
    public String loadPrompt(String name, Map<String, String> vars) {
        try {
            // Try prompts directory relative to working dir, then classpath
            Path promptPath = Paths.get("prompts", name + ".txt");
            String content;
            if (Files.exists(promptPath)) {
                content = new String(Files.readAllBytes(promptPath), StandardCharsets.UTF_8);
            } else {
                // Fall back to classpath
                java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("prompts/" + name + ".txt");
                if (is == null) throw new RuntimeException("Prompt not found: " + name);
                content = readAll(is, StandardCharsets.UTF_8);
                is.close();
            }
            if (vars != null) {
                for (Map.Entry<String, String> e : vars.entrySet()) {
                    content = content.replace("{{" + e.getKey() + "}}", e.getValue());
                }
            }
            return content;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load prompt: " + name, e);
        }
    }

    /**
     * Smart AI call: uses streaming if thinking is enabled, non-streaming otherwise.
     */
    public String callAISmart(String systemPrompt, String userMessage, StreamCallback callback,
                              boolean enableThinking, int maxTokens, double temperature) throws Exception {
        if (!enableThinking) {
            String result = callAINonStream(systemPrompt, userMessage, maxTokens, temperature);
            if (callback != null) callback.onChunk(result);
            return result;
        }

        try {
            return callAIStream(systemPrompt, userMessage, callback, maxTokens, temperature);
        } catch (Exception e) {
            log.warn("Streaming failed, falling back to non-streaming: {}", e.getMessage());
            String result = callAINonStream(systemPrompt, userMessage, maxTokens, temperature);
            if (callback != null) callback.onChunk(result);
            return result;
        }
    }

    /**
     * Non-streaming AI call.
     */
    public String callAINonStream(String systemPrompt, String userMessage, int maxTokens, double temperature) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());
        body.put("max_tokens", maxTokens > 0 ? maxTokens : config.getMaxTokens());
        body.put("temperature", temperature >= 0 ? temperature : config.getTemperature());
        body.put("system", systemPrompt);
        body.put("messages", java.util.Collections.singletonList(
                com.novel.comic.util.Maps.of("role", "user", "content", userMessage)));
        body.put("stream", false);
        body.put("thinking", com.novel.comic.util.Maps.of("type", "disabled"));

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/v1/messages")
                .header("Content-Type", "application/json")
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .post(RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(body)))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : response.message();
                throw new RuntimeException("API " + response.code() + ": " + errBody);
            }
            String respBody = response.body().string();
            JsonNode root = mapper.readTree(respBody);
            StringBuilder sb = new StringBuilder();
            JsonNode content = root.get("content");
            if (content != null && content.isArray()) {
                for (JsonNode block : content) {
                    if ("text".equals(block.get("type").asText())) {
                        sb.append(block.get("text").asText());
                    }
                }
            }
            return sb.toString();
        }
    }

    /**
     * Streaming AI call, parsing Anthropic SSE format.
     */
    public String callAIStream(String systemPrompt, String userMessage, StreamCallback callback,
                                int maxTokens, double temperature) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());
        body.put("max_tokens", maxTokens > 0 ? maxTokens : config.getMaxTokens());
        body.put("temperature", temperature >= 0 ? temperature : config.getTemperature());
        body.put("system", systemPrompt);
        body.put("messages", java.util.Collections.singletonList(
                com.novel.comic.util.Maps.of("role", "user", "content", userMessage)));
        body.put("stream", true);
        body.put("thinking", com.novel.comic.util.Maps.of("type", "enabled", "budget_tokens", 2048));

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/v1/messages")
                .header("Content-Type", "application/json")
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .post(RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(body)))
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String errBody = response.body() != null ? response.body().string() : response.message();
            response.close();
            throw new RuntimeException("API " + response.code() + ": " + errBody);
        }

        StringBuilder fullText = new StringBuilder();
        StringBuilder thinking = new StringBuilder();
        int thinkingCount = 0;
        boolean thinkingDone = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) continue;
                String json = line.substring(6);
                try {
                    JsonNode node = mapper.readTree(json);
                    String type = node.has("type") ? node.get("type").asText() : "";

                    if ("content_block_start".equals(type)) {
                        JsonNode cb = node.get("content_block");
                        if (cb != null && "thinking".equals(cb.get("type").asText())) {
                            if (callback != null) callback.onStatus("开始思考...");
                        }
                    } else if ("content_block_delta".equals(type)) {
                        JsonNode delta = node.get("delta");
                        if (delta != null) {
                            String dt = delta.has("type") ? delta.get("type").asText() : "";
                            if ("text_delta".equals(dt) && delta.has("text")) {
                                String text = delta.get("text").asText();
                                fullText.append(text);
                                if (callback != null) callback.onChunk(text);
                            } else if (("thinking_delta".equals(dt) || "signature_delta".equals(dt))
                                    && delta.has("thinking")) {
                                thinking.append(delta.get("thinking").asText());
                                thinkingCount++;
                                if (thinkingCount % 5 == 0 && callback != null) {
                                    callback.onStatus("思考中... (已思考" + thinking.length() + "字)");
                                }
                            }
                        }
                    } else if ("content_block_stop".equals(type)) {
                        if (!thinkingDone) {
                            thinkingDone = true;
                            if (callback != null) callback.onStatus("思考完成 (共" + thinking.length() + "字)");
                        }
                    } else if ("message_delta".equals(type)) {
                        JsonNode d = node.get("delta");
                        if (d != null && d.has("stop_reason")) {
                            // stream ending
                        }
                    } else if ("message_stop".equals(type)) {
                        // done
                    }
                } catch (Exception e) {
                    // skip malformed JSON chunks
                }
            }
        } finally {
            response.close();
        }

        String result = fullText.toString();
        if (result.isEmpty()) throw new RuntimeException("API 未返回任何文本内容");
        return result;
    }

    // Java 8 compatible readAllBytes
    private static String readAll(InputStream is, java.nio.charset.Charset cs) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int n;
        while ((n = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, n);
        }
        return new String(buffer.toByteArray(), cs);
    }
}