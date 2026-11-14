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
 * Anthropic Claude API provider implementation.
 * Supports: CHAT, VISION
 * Note: Claude does not support IMAGE_GEN natively.
 */
public class ClaudeProvider implements GatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeProvider.class);
    private static final String PROVIDER_NAME = "CLAUDE";

    private static final List<ModelSpec> MODELS = Collections.unmodifiableList(Arrays.asList(
            new ModelSpec("claude-opus-5-5", "Claude Opus 5.5", ProviderType.CLAUDE,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("claude-sonnet-5-5", "Claude Sonnet 5.5", ProviderType.CLAUDE,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("claude-haiku-5-5", "Claude Haiku 5.5", ProviderType.CLAUDE,
                    EnumSet.of(Capability.CHAT, Capability.VISION))
    ));

    private final ProviderConfig.ProviderSettings settings;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final ProviderRequestLogger requestLogger;

    public ClaudeProvider(ProviderConfig.ProviderSettings settings, ProviderRequestLogger requestLogger) {
        this.settings = settings;
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
        this.requestLogger = requestLogger;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.CLAUDE;
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

            ResponseEntity<String> resp = doPost(settings.getBaseUrl() + "/v1/messages", body);
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
            log.error("Claude chat error", e);
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

            restTemplate.execute(
                    settings.getBaseUrl() + "/v1/messages",
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
                                    String type = event.path("type").asText();

                                    if ("content_block_delta".equals(type)) {
                                        JsonNode delta = event.path("delta");
                                        String content = delta.path("text").asText(null);
                                        ChatStreamChunk chunk = new ChatStreamChunk(
                                                event.path("id").asText(), content, null, null);
                                        onChunk.accept(chunk);
                                    } else if ("message_stop".equals(type)) {
                                        long duration = System.currentTimeMillis() - startTime;
                                        requestLogger.logChatRequestSuccess(PROVIDER_NAME, modelName, duration, "[stream completed]", run);
                                        break;
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
            log.error("Claude stream error", e);
            onError.accept(e);
        }
    }

    @Override
    public ImageGenResult imageGen(ImageGenCommand command, String modelName) {
        TraceContext.setModel(modelName);
        LangSmithRun run = requestLogger.logImageGenRequestStart(PROVIDER_NAME, modelName, command);
        requestLogger.logImageGenRequestError(PROVIDER_NAME, modelName, 0, "Claude does not support image generation", run);
        return ImageGenResult.builder()
                .error("Claude does not support image generation")
                .build();
    }

    // --- Private helpers ---

    private ObjectNode buildChatRequestBody(ChatCommand command, String modelName) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", modelName);
        body.put("max_tokens", command.getMaxTokens() != null ? command.getMaxTokens() : 4096);

        // Extract system message
        StringBuilder systemMsg = new StringBuilder();
        ArrayNode msgs = body.putArray("messages");

        for (ChatCommand.Message msg : command.getMessages()) {
            if ("system".equals(msg.getRole())) {
                if (systemMsg.length() > 0) systemMsg.append("\n");
                systemMsg.append(msg.getTextContent());
                continue;
            }

            ObjectNode msgNode = msgs.addObject();
            msgNode.put("role", mapRole(msg.getRole()));

            // Check for images
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
                        partNode.put("type", "image");
                        ObjectNode source = partNode.putObject("source");
                        source.put("type", "url");
                        source.put("url", part.getImageUrl());
                    }
                }
            } else {
                msgNode.put("content", msg.getTextContent());
            }
        }

        if (systemMsg.length() > 0) {
            body.put("system", systemMsg.toString());
        }

        if (command.getTemperature() != null) {
            body.put("temperature", command.getTemperature());
        }

        return body;
    }

    private ChatResult parseChatResponse(JsonNode json) {
        String id = json.path("id").asText();
        String finishReason = mapFinishReason(json.path("stop_reason").asText(null));

        StringBuilder content = new StringBuilder();
        JsonNode contentArray = json.path("content");
        if (contentArray.isArray()) {
            for (JsonNode block : contentArray) {
                if ("text".equals(block.path("type").asText())) {
                    content.append(block.path("text").asText());
                }
            }
        }

        ChatResult.UsageInfo usage = null;
        JsonNode usageNode = json.path("usage");
        if (!usageNode.isMissingNode()) {
            usage = new ChatResult.UsageInfo(
                    usageNode.path("input_tokens").asInt(),
                    usageNode.path("output_tokens").asInt(),
                    usageNode.path("input_tokens").asInt() + usageNode.path("output_tokens").asInt()
            );
        }

        return ChatResult.builder()
                .id(id)
                .content(content.toString())
                .finishReason(finishReason)
                .usage(usage)
                .build();
    }

    private String mapRole(String openaiRole) {
        switch (openaiRole) {
            case "assistant": return "assistant";
            default: return "user";
        }
    }

    private String mapFinishReason(String claudeReason) {
        if (claudeReason == null) return null;
        switch (claudeReason) {
            case "end_turn": return "stop";
            case "max_tokens": return "length";
            default: return claudeReason;
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", settings.getApiKey());
        headers.set("anthropic-version", "2023-06-01");
        return headers;
    }

    private ResponseEntity<String> doPost(String url, ObjectNode body) throws Exception {
        HttpHeaders headers = buildHeaders();
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }
}
