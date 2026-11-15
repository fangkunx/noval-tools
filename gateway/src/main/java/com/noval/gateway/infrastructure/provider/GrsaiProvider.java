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
 * Grsai API Provider Implementation.
 * Supports: CHAT, VISION, IMAGE_GEN
 *
 * Base URLs:
 * - Global: https://grsaiapi.com
 * - China: https://grsai.dakka.com.cn
 *
 * API Endpoints (OpenAI compatible):
 * - /v1/chat/completions
 * - /v1/images/generations
 * - /v1/images/edits
 */
public class GrsaiProvider implements GatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(GrsaiProvider.class);
    private static final String PROVIDER_NAME = "GRSAI";

    /**
     * Nano Banana series models for image generation
     */
    private static final List<ModelSpec> MODELS = Collections.unmodifiableList(Arrays.asList(
            // Nano Banana series - Image generation models
            new ModelSpec("nano-banana", "Nano Banana", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("nano-banana-fast", "Nano Banana Fast", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("nano-banana-2", "Nano Banana 2", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("nano-banana-2-cl", "Nano Banana 2 CL", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("nano-banana-2-2k-cl", "Nano Banana 2 2K CL", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("nano-banana-2-4k-cl", "Nano Banana 2 4K CL", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("nano-banana-2.1", "Nano Banana 2.1", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("nano-banana-pro", "Nano Banana Pro", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("nano-banana-pro-vt", "Nano Banana Pro VT", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("nano-banana-pro-cl", "Nano Banana Pro CL", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("nano-banana-pro-vip", "Nano Banana Pro VIP", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("nano-banana-pro-4k-vip", "Nano Banana Pro 4K VIP", ProviderType.GRSATI,
                    EnumSet.of(Capability.IMAGE_GEN)),
            // Standard OpenAI-compatible models (if supported by Grsai)
            new ModelSpec("gpt-4o", "GPT-4o (Grsai)", ProviderType.GRSATI,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("gpt-4o-mini", "GPT-4o Mini (Grsai)", ProviderType.GRSATI,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("claude-sonnet-5-5", "Claude Sonnet 5.5 (Grsai)", ProviderType.GRSATI,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("gemini-2.0-flash", "Gemini 2.0 Flash (Grsai)", ProviderType.GRSATI,
                    EnumSet.of(Capability.CHAT, Capability.VISION))
    ));

    private final ProviderConfig.ProviderSettings settings;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final ProviderRequestLogger requestLogger;

    public GrsaiProvider(ProviderConfig.ProviderSettings settings, ProviderRequestLogger requestLogger) {
        this.settings = settings;
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
        this.requestLogger = requestLogger;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.GRSATI;
    }

    @Override
    public List<ModelSpec> getSupportedModels() {
        return MODELS;
    }

    @Override
    public Set<Capability> getSupportedCapabilities() {
        return EnumSet.of(Capability.CHAT, Capability.VISION, Capability.IMAGE_GEN);
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
            ObjectNode body = buildChatRequestBody(command, modelName);
            body.put("stream", false);

            ResponseEntity<String> resp = doPost(getBaseUrl() + "/v1/chat/completions", body);
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
            log.error("Grsai chat error", e);
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
            ObjectNode body = buildChatRequestBody(command, modelName);
            body.put("stream", true);

            HttpHeaders headers = buildHeaders();
            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

            restTemplate.execute(
                    getBaseUrl() + "/v1/chat/completions",
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
                                    if ("[DONE]".equals(data)) {
                                        long duration = System.currentTimeMillis() - startTime;
                                        requestLogger.logChatRequestSuccess(PROVIDER_NAME, modelName, duration, "[stream completed]", run);
                                        break;
                                    }
                                    JsonNode chunk = mapper.readTree(data);
                                    ChatStreamChunk streamChunk = parseStreamChunk(chunk);
                                    if (streamChunk != null) {
                                        onChunk.accept(streamChunk);
                                    }
                                }
                            }
                        }
                        return null;
                    }
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            requestLogger.logChatRequestError(PROVIDER_NAME, modelName, duration, e.getMessage(), run);
            log.error("Grsai stream error", e);
            onError.accept(e);
        }
    }

    @Override
    public ImageGenResult imageGen(ImageGenCommand command, String modelName) {
        TraceContext.setModel(modelName);
        long startTime = System.currentTimeMillis();

        LangSmithRun run = requestLogger.logImageGenRequestStart(PROVIDER_NAME, modelName, command);

        try {
            ObjectNode body = mapper.createObjectNode();

            // Determine model: use command model if specified, otherwise use provided modelName, or default
            String model = command.getModel() != null ? command.getModel() :
                          (modelName != null ? modelName : "nano-banana-2");
            body.put("model", model);
            body.put("prompt", command.getPrompt());

            // Grsai specific parameters
            if (command.getN() != null) {
                body.put("n", command.getN());
            }
            if (command.getSize() != null) {
                body.put("size", command.getSize());
            }
            if (command.getResponseFormat() != null) {
                body.put("response_format", command.getResponseFormat());
            }

            ResponseEntity<String> resp = doPost(getBaseUrl() + "/v1/images/generations", body);
            JsonNode json = mapper.readTree(resp.getBody());

            String id = json.path("id").asText(json.path("created").asText());
            List<String> urls = new ArrayList<>();
            List<String> b64 = new ArrayList<>();

            JsonNode dataArray = json.path("data");
            if (dataArray.isArray()) {
                for (JsonNode item : dataArray) {
                    String url = item.path("url").asText(null);
                    String b64Data = item.path("b64_json").asText(null);
                    if (url != null && !url.isEmpty()) urls.add(url);
                    if (b64Data != null && !b64Data.isEmpty()) b64.add(b64Data);
                }
            }

            // Also check for result_url or result_urls format (some Grsai endpoints)
            String resultUrl = json.path("result_url").asText(null);
            if (resultUrl != null && !resultUrl.isEmpty()) {
                urls.add(resultUrl);
            }
            JsonNode resultUrls = json.path("result_urls");
            if (resultUrls.isArray()) {
                for (JsonNode url : resultUrls) {
                    String urlText = url.asText(null);
                    if (urlText != null && !urlText.isEmpty()) {
                        urls.add(urlText);
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            if (urls.isEmpty() && b64.isEmpty()) {
                String error = "No images returned from Grsai";
                JsonNode errorNode = json.path("error");
                if (!errorNode.isMissingNode()) {
                    error = "Grsai error: " + (errorNode.isObject() ?
                            errorNode.path("message").asText(errorNode.toString()) :
                            errorNode.asText(error));
                }
                requestLogger.logImageGenRequestError(PROVIDER_NAME, modelName, duration, error, run);
                return ImageGenResult.builder().error(error).build();
            }

            requestLogger.logImageGenRequestSuccess(PROVIDER_NAME, modelName, duration, urls.size() + b64.size(), run);

            return ImageGenResult.builder()
                    .id(id)
                    .urls(urls)
                    .base64Data(b64)
                    .build();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            requestLogger.logImageGenRequestError(PROVIDER_NAME, modelName, duration, e.getMessage(), run);
            log.error("Grsai image gen error", e);
            return ImageGenResult.builder().error(e.getMessage()).build();
        }
    }

    // --- Private helpers ---

    private String getBaseUrl() {
        String baseUrl = settings.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            // Default to global endpoint
            return "https://grsaiapi.com";
        }
        // Remove trailing slash if present
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private ObjectNode buildChatRequestBody(ChatCommand command, String modelName) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", modelName);

        ArrayNode msgs = body.putArray("messages");
        for (ChatCommand.Message msg : command.getMessages()) {
            ObjectNode msgNode = msgs.addObject();
            msgNode.put("role", msg.getRole());

            // Check if we need content parts (vision) or simple string
            boolean hasImage = msg.getContent().stream()
                    .anyMatch(p -> p.getType() == ChatCommand.ContentPart.Type.IMAGE_URL);

            if (hasImage) {
                ArrayNode contentParts = msgNode.putArray("content");
                for (ChatCommand.ContentPart part : msg.getContent()) {
                    ObjectNode partNode = contentParts.addObject();
                    if (part.getType() == ChatCommand.ContentPart.Type.TEXT) {
                        partNode.put("type", "text");
                        partNode.put("text", part.getText());
                    } else {
                        partNode.put("type", "image_url");
                        ObjectNode imageUrl = partNode.putObject("image_url");
                        imageUrl.put("url", part.getImageUrl());
                    }
                }
            } else {
                msgNode.put("content", msg.getTextContent());
            }
        }

        if (command.getTemperature() != null) {
            body.put("temperature", command.getTemperature());
        }
        if (command.getMaxTokens() != null) {
            body.put("max_tokens", command.getMaxTokens());
        }

        return body;
    }

    private ChatResult parseChatResponse(JsonNode json) {
        // Check for error first
        JsonNode errorNode = json.path("error");
        if (!errorNode.isMissingNode()) {
            String errorMsg = errorNode.isObject() ?
                    errorNode.path("message").asText(errorNode.toString()) :
                    errorNode.asText("Unknown error");
            return ChatResult.builder().error(errorMsg).build();
        }

        String id = json.path("id").asText();
        JsonNode choices = json.path("choices");
        if (choices.isEmpty()) {
            return ChatResult.builder().error("No choices in response").build();
        }

        String finishReason = choices.get(0).path("finish_reason").asText(null);
        String content = choices.get(0).path("message").path("content").asText("");

        ChatResult.UsageInfo usage = null;
        JsonNode usageNode = json.path("usage");
        if (!usageNode.isMissingNode()) {
            usage = new ChatResult.UsageInfo(
                    usageNode.path("prompt_tokens").asInt(),
                    usageNode.path("completion_tokens").asInt(),
                    usageNode.path("total_tokens").asInt()
            );
        }

        return ChatResult.builder()
                .id(id)
                .content(content)
                .finishReason(finishReason)
                .usage(usage)
                .build();
    }

    private ChatStreamChunk parseStreamChunk(JsonNode json) {
        JsonNode choices = json.path("choices");
        if (choices.isEmpty()) return null;

        String id = json.path("id").asText();
        JsonNode delta = choices.get(0).path("delta");
        String content = delta.path("content").asText(null);
        String finishReason = choices.get(0).path("finish_reason").asText(null);

        return new ChatStreamChunk(id, content, finishReason, null);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String apiKey = settings.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.setBearerAuth(apiKey);
        }

        return headers;
    }

    private ResponseEntity<String> doPost(String url, ObjectNode body) throws Exception {
        HttpHeaders headers = buildHeaders();
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }
}
