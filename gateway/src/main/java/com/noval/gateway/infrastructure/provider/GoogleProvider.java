package com.noval.gateway.infrastructure.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noval.gateway.domain.model.*;
import com.noval.gateway.domain.spi.GatewayProvider;
import com.noval.gateway.infrastructure.config.ProviderConfig;
import com.noval.gateway.infrastructure.logging.ProviderRequestLogger;
import com.noval.gateway.infrastructure.logging.TraceContext;
import com.noval.gateway.infrastructure.tracing.langsmith.LangSmithRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Consumer;

/**
 * Google Gemini API provider implementation.
 * Supports: CHAT, VISION
 * Note: Gemini does not support IMAGE_GEN natively (uses Imagen for that).
 */
public class GoogleProvider implements GatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleProvider.class);
    private static final String PROVIDER_NAME = "GOOGLE";

    private static final List<ModelSpec> MODELS = Collections.unmodifiableList(Arrays.asList(
            new ModelSpec("gemini-2.0-flash", "Gemini 2.0 Flash", ProviderType.GOOGLE,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("gemini-1.5-pro", "Gemini 1.5 Pro", ProviderType.GOOGLE,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("gemini-1.5-flash", "Gemini 1.5 Flash", ProviderType.GOOGLE,
                    EnumSet.of(Capability.CHAT, Capability.VISION))
    ));

    private final ProviderConfig.ProviderSettings settings;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final ProviderRequestLogger requestLogger;

    public GoogleProvider(ProviderConfig.ProviderSettings settings, ProviderRequestLogger requestLogger) {
        this.settings = settings;
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
        this.requestLogger = requestLogger;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.GOOGLE;
    }

    @Override
    public List<ModelSpec> getSupportedModels() {
        return MODELS;
    }

    @Override
    public Set<Capability> getSupportedCapabilities() {
        return EnumSet.of(Capability.CHAT, Capability.VISION);
    }

    @Override
    public boolean supportsModel(String modelId) {
        return MODELS.stream().anyMatch(m -> m.getId().equals(modelId));
    }

    @Override
    public ChatResult chat(ChatCommand command, String modelName) {
        TraceContext.setModel(modelName);
        long startTime = System.currentTimeMillis();

        LangSmithRun run = requestLogger.logChatRequestStart(PROVIDER_NAME, modelName, command);

        try {
            ObjectNode body = buildChatRequestBody(command);
            String url = String.format("%s/v1beta/models/%s:generateContent?key=%s",
                    settings.getBaseUrl(), modelName, settings.getApiKey());

            ResponseEntity<String> resp = doPost(url, body);
            JsonNode json = mapper.readTree(resp.getBody());

            ChatResult result = parseChatResponse(json);
            long duration = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                requestLogger.logChatRequestSuccess(PROVIDER_NAME, modelName, duration, result.getContent(), run);
            } else {
                requestLogger.logChatRequestError(PROVIDER_NAME, modelName, duration, result.getError(), run);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            requestLogger.logChatRequestError(PROVIDER_NAME, modelName, duration, e.getMessage(), run);
            log.error("Gemini chat error", e);
            return ChatResult.builder().error(e.getMessage()).build();
        }
    }

    @Override
    public void chatStream(ChatCommand command, String modelName,
                           Consumer<ChatStreamChunk> onChunk, Consumer<Throwable> onError) {
        TraceContext.setModel(modelName);
        long startTime = System.currentTimeMillis();

        LangSmithRun run = requestLogger.logChatRequestStart(PROVIDER_NAME, modelName, command);

        try {
            ObjectNode body = buildChatRequestBody(command);
            String url = String.format("%s/v1beta/models/%s:streamGenerateContent?alt=sse&key=%s",
                    settings.getBaseUrl(), modelName, settings.getApiKey());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.execute(
                    url,
                    HttpMethod.POST,
                    req -> {
                        req.getHeaders().putAll(headers);
                        mapper.writeValue(req.getBody(), body);
                    },
                    response -> {
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(response.getBody()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    JsonNode event = mapper.readTree(data);
                                    ChatStreamChunk chunk = parseStreamChunk(event);
                                    if (chunk != null && chunk.getContent() != null) {
                                        onChunk.accept(chunk);
                                    }
                                }
                            }
                        }
                        long duration = System.currentTimeMillis() - startTime;
                        requestLogger.logChatRequestSuccess(PROVIDER_NAME, modelName, duration, "[stream completed]", run);
                        return null;
                    }
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            requestLogger.logChatRequestError(PROVIDER_NAME, modelName, duration, e.getMessage(), run);
            log.error("Gemini stream error", e);
            onError.accept(e);
        }
    }

    @Override
    public ImageGenResult imageGen(ImageGenCommand command, String modelName) {
        TraceContext.setModel(modelName);
        LangSmithRun run = requestLogger.logImageGenRequestStart(PROVIDER_NAME, modelName, command);
        requestLogger.logImageGenRequestError(PROVIDER_NAME, modelName, 0, "Google Gemini does not support image generation natively", run);
        return ImageGenResult.builder()
                .error("Google Gemini does not support image generation natively. Use Imagen API instead.")
                .build();
    }

    // --- Private helpers ---

    private ObjectNode buildChatRequestBody(ChatCommand command) {
        ObjectNode body = mapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");

        for (ChatCommand.Message msg : command.getMessages()) {
            if ("system".equals(msg.getRole())) {
                // Gemini uses systemInstruction
                ObjectNode sysInst = body.putObject("systemInstruction");
                ArrayNode sysParts = sysInst.putArray("parts");
                ObjectNode textPart = sysParts.addObject();
                textPart.put("text", msg.getTextContent());
                continue;
            }

            ObjectNode contentNode = contents.addObject();
            contentNode.put("role", mapRole(msg.getRole()));
            ArrayNode parts = contentNode.putArray("parts");

            for (ChatCommand.ContentPart part : msg.getContent()) {
                ObjectNode partNode = parts.addObject();
                if (part.getType() == ChatCommand.ContentPart.Type.TEXT) {
                    partNode.put("text", part.getText());
                } else {
                    // Image URL — Gemini requires base64 or file_data
                    ObjectNode inlineData = partNode.putObject("inlineData");
                    inlineData.put("mimeType", "image/jpeg");
                    inlineData.put("data", part.getImageUrl()); // Assume base64 for now
                }
            }
        }

        // Generation config
        ObjectNode genConfig = body.putObject("generationConfig");
        if (command.getTemperature() != null) {
            genConfig.put("temperature", command.getTemperature());
        }
        if (command.getMaxTokens() != null) {
            genConfig.put("maxOutputTokens", command.getMaxTokens());
        }

        return body;
    }

    private ChatResult parseChatResponse(JsonNode json) {
        JsonNode candidates = json.path("candidates");
        if (candidates.isEmpty()) {
            return ChatResult.builder().error("No candidates returned").build();
        }

        JsonNode candidate = candidates.get(0);
        String finishReason = mapFinishReason(candidate.path("finishReason").asText(null));

        StringBuilder content = new StringBuilder();
        JsonNode parts = candidate.path("content").path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                content.append(part.path("text").asText());
            }
        }

        ChatResult.UsageInfo usage = null;
        JsonNode usageNode = json.path("usageMetadata");
        if (!usageNode.isMissingNode()) {
            usage = new ChatResult.UsageInfo(
                    usageNode.path("promptTokenCount").asInt(),
                    usageNode.path("candidatesTokenCount").asInt(),
                    usageNode.path("totalTokenCount").asInt()
            );
        }

        return ChatResult.builder()
                .id(UUID.randomUUID().toString())
                .content(content.toString())
                .finishReason(finishReason)
                .usage(usage)
                .build();
    }

    private ChatStreamChunk parseStreamChunk(JsonNode json) {
        JsonNode candidates = json.path("candidates");
        if (candidates.isEmpty()) return null;

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (parts.isEmpty()) return null;

        String content = parts.get(0).path("text").asText(null);
        String finishReason = mapFinishReason(candidates.get(0).path("finishReason").asText(null));

        return new ChatStreamChunk(UUID.randomUUID().toString(), content, finishReason, null);
    }

    private String mapRole(String openaiRole) {
        switch (openaiRole) {
            case "assistant": return "model";
            default: return "user";
        }
    }

    private String mapFinishReason(String geminiReason) {
        if (geminiReason == null) return null;
        switch (geminiReason) {
            case "STOP": return "stop";
            case "MAX_TOKENS": return "length";
            default: return geminiReason;
        }
    }

    private ResponseEntity<String> doPost(String url, ObjectNode body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }
}
