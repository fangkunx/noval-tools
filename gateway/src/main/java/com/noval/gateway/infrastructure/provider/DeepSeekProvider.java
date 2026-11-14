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
 * DeepSeek API provider implementation.
 * DeepSeek is compatible with OpenAI Chat Completions API.
 * Supports: CHAT, VISION
 * Note: DeepSeek does not support IMAGE_GEN natively.
 */
public class DeepSeekProvider implements GatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekProvider.class);
    private static final String PROVIDER_NAME = "DEEPSEEK";

    private static final List<ModelSpec> MODELS = Collections.unmodifiableList(Arrays.asList(
            new ModelSpec("deepseek-chat", "DeepSeek Chat", ProviderType.DEEPSEEK,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("deepseek-flash", "DeepSeek Chat", ProviderType.DEEPSEEK,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("deepseek-pro", "DeepSeek Chat", ProviderType.DEEPSEEK,
                    EnumSet.of(Capability.CHAT, Capability.VISION)),
            new ModelSpec("deepseek-reasoner", "DeepSeek Reasoner", ProviderType.DEEPSEEK,
                    EnumSet.of(Capability.CHAT))
    ));

    private final ProviderConfig.ProviderSettings settings;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final ProviderRequestLogger requestLogger;

    public DeepSeekProvider(ProviderConfig.ProviderSettings settings, ProviderRequestLogger requestLogger) {
        this.settings = settings;
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
        this.requestLogger = requestLogger;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.DEEPSEEK;
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

            ResponseEntity<String> resp = doPost(settings.getBaseUrl() + "/v1/chat/completions", body);
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
            log.error("DeepSeek chat error", e);
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
                    settings.getBaseUrl() + "/v1/chat/completions",
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
            log.error("DeepSeek stream error", e);
            onError.accept(e);
        }
    }

    @Override
    public ImageGenResult imageGen(ImageGenCommand command, String modelName) {
        TraceContext.setModel(modelName);
        LangSmithRun run = requestLogger.logImageGenRequestStart(PROVIDER_NAME, modelName, command);
        requestLogger.logImageGenRequestError(PROVIDER_NAME, modelName, 0, "DeepSeek does not support image generation", run);
        return ImageGenResult.builder()
                .error("DeepSeek does not support image generation")
                .build();
    }

    // --- Private helpers ---

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
        String id = json.path("id").asText();
        String finishReason = json.path("choices").get(0).path("finish_reason").asText(null);
        String content = json.path("choices").get(0).path("message").path("content").asText("");

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
        String id = json.path("id").asText();
        JsonNode choices = json.path("choices");
        if (choices.isEmpty()) return null;

        JsonNode delta = choices.get(0).path("delta");
        String content = delta.path("content").asText(null);
        String finishReason = choices.get(0).path("finish_reason").asText(null);

        return new ChatStreamChunk(id, content, finishReason, null);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(settings.getApiKey());
        return headers;
    }

    private ResponseEntity<String> doPost(String url, ObjectNode body) throws Exception {
        HttpHeaders headers = buildHeaders();
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }
}
