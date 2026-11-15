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
 * KL Chat Provider (apikl.ai)
 * Supports: CHAT, VISION
 *
 * This provider uses a dedicated API key for chat operations.
 * Configure via: gateway.providers.kl-chat.api-key
 */
public class KlChatProvider implements GatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(KlChatProvider.class);
    private static final String PROVIDER_NAME = "KL_CHAT";

    /**
     * Supported models for chat and vision
     */
    private static final List<ModelSpec> MODELS = Collections.unmodifiableList(Arrays.asList(
            new ModelSpec("gpt-4o", "GPT-4o (KL)", ProviderType.KL_CHAT,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("gpt-4o-mini", "GPT-4o Mini (KL)", ProviderType.KL_CHAT,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("gpt-4-turbo", "GPT-4 Turbo (KL)", ProviderType.KL_CHAT,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("claude-sonnet-5-5", "Claude Sonnet 5.5 (KL)", ProviderType.KL_CHAT,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("claude-opus-5-5", "Claude Opus 5.5 (KL)", ProviderType.KL_CHAT,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("gemini-2.0-flash", "Gemini 2.0 Flash (KL)", ProviderType.KL_CHAT,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("gemini-1.5-pro", "Gemini 1.5 Pro (KL)", ProviderType.KL_CHAT,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("deepseek-chat", "DeepSeek Chat (KL)", ProviderType.KL_CHAT,
                    EnumSet.of(Capability.CHAT)),
            new ModelSpec("deepseek-reasoner", "DeepSeek Reasoner (KL)", ProviderType.KL_CHAT,
                    EnumSet.of(Capability.CHAT))
    ));

    private final ProviderConfig.ProviderSettings settings;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final ProviderRequestLogger requestLogger;

    public KlChatProvider(ProviderConfig.ProviderSettings settings, ProviderRequestLogger requestLogger) {
        this.settings = settings;
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
        this.requestLogger = requestLogger;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.KL_CHAT;
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
            log.error("KL Chat error", e);
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
            log.error("KL Chat stream error", e);
            onError.accept(e);
        }
    }

    @Override
    public ImageGenResult imageGen(ImageGenCommand command, String modelName) {
        return ImageGenResult.builder().error("KL Chat provider does not support image generation. Use KL Image provider.").build();
    }

    // --- Private helpers ---

    private String getBaseUrl() {
        String baseUrl = settings.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "https://apikl.ai";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private ObjectNode buildChatRequestBody(ChatCommand command, String modelName) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", modelName);

        ArrayNode msgs = body.putArray("messages");
        for (ChatCommand.Message msg : command.getMessages()) {
            ObjectNode msgNode = msgs.addObject();
            msgNode.put("role", msg.getRole());

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
