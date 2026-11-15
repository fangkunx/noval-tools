package com.noval.gateway.infrastructure.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * KL Image Provider (apikl.ai)
 * Supports: IMAGE_GEN, IMAGE_EDIT
 *
 * This provider uses a dedicated API key for image generation operations.
 * Configure via: gateway.providers.kl-image.api-key
 */
public class KlImageProvider implements GatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(KlImageProvider.class);
    private static final String PROVIDER_NAME = "KL_IMAGE";

    /**
     * Supported models for image generation and editing
     * KL model groups: https://docs.apikl.ai
     */
    private static final List<ModelSpec> MODELS = Collections.unmodifiableList(Arrays.asList(
            new ModelSpec("dall-e-3", "DALL-E 3 (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("dall-e-2", "DALL-E 2 (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("gpt-image-1", "GPT Image 1 (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN, Capability.IMAGE_EDIT)),
            // gpt-image-2.5 系列（需使用生图分组 Key）
            new ModelSpec("gpt-image-2.5-flare", "GPT Image 2.5 Flare (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("gpt-image-2.5-sunburst", "GPT Image 2.5 Sunburst (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("gpt-image-2.5-flare-4k", "GPT Image 2.5 Flare 4K (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("gpt-image-2.5-sunburst-4k", "GPT Image 2.5 Sunburst 4K (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("gpt-image-2.5", "GPT Image 2.5 (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN, Capability.IMAGE_EDIT)),
            // gpt-image-2 系列
            new ModelSpec("gpt-image-2", "GPT Image 2 (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN)),
            // Flux 系列
            new ModelSpec("flux-pro", "Flux Pro (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN, Capability.IMAGE_EDIT)),
            new ModelSpec("flux-dev", "Flux Dev (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN, Capability.IMAGE_EDIT)),
            new ModelSpec("flux-schnell", "Flux Schnell (KL)", ProviderType.KL_IMAGE,
                    EnumSet.of(Capability.IMAGE_GEN, Capability.IMAGE_EDIT))
    ));

    // gpt-image-2.5 系列默认 2K，4k 系列默认 4K
    private static final String DEFAULT_SIZE_2K = "2048x2048";
    private static final String DEFAULT_SIZE_4K = "3840x2160";

    private final ProviderConfig.ProviderSettings settings;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final ProviderRequestLogger requestLogger;

    public KlImageProvider(ProviderConfig.ProviderSettings settings, ProviderRequestLogger requestLogger) {
        this.settings = settings;
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
        this.requestLogger = requestLogger;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.KL_IMAGE;
    }

    @Override
    public List<ModelSpec> getSupportedModels() {
        return MODELS;
    }

    @Override
    public Set<Capability> getSupportedCapabilities() {
        return EnumSet.of(Capability.IMAGE_GEN, Capability.IMAGE_EDIT);
    }

    @Override
    public boolean supportsModel(String modelId) {
        return MODELS.stream().anyMatch(m -> m.getId().equals(modelId));
    }

    @Override
    public ChatResult chat(ChatCommand command, String modelName) {
        return ChatResult.builder().error("KL Image provider does not support chat. Use KL Chat provider.").build();
    }

    @Override
    public void chatStream(ChatCommand command, String modelName,
                           Consumer<ChatStreamChunk> onChunk, Consumer<Throwable> onError) {
        onError.accept(new UnsupportedOperationException("KL Image provider does not support chat"));
    }

    @Override
    public ImageGenResult imageGen(ImageGenCommand command, String modelName) {
        TraceContext.setModel(modelName);
        long startTime = System.currentTimeMillis();

        LangSmithRun run = requestLogger.logImageGenRequestStart(PROVIDER_NAME, modelName, command);

        try {
            ObjectNode body = mapper.createObjectNode();

            String model = command.getModel() != null ? command.getModel() :
                          (modelName != null ? modelName : "dall-e-3");
            body.put("model", model);
            body.put("prompt", command.getPrompt());

            if (command.getN() != null) {
                body.put("n", command.getN());
            }
            // Apply default size based on model if not specified
            if (command.getSize() != null) {
                body.put("size", command.getSize());
            } else {
                body.put("size", getDefaultSize(model));
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

            long duration = System.currentTimeMillis() - startTime;

            if (urls.isEmpty() && b64.isEmpty()) {
                String error = "No images returned from KL";
                JsonNode errorNode = json.path("error");
                if (!errorNode.isMissingNode()) {
                    error = "KL error: " + (errorNode.isObject() ?
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
            log.error("KL Image gen error", e);
            return ImageGenResult.builder().error(e.getMessage()).build();
        }
    }

    @Override
    public ImageGenResult imageEdit(ImageEditCommand command, String modelName) {
        TraceContext.setModel(modelName);
        long startTime = System.currentTimeMillis();

        try {
            ObjectNode body = mapper.createObjectNode();

            String model = command.getModel() != null ? command.getModel() :
                          (modelName != null ? modelName : "gpt-image-1");
            body.put("model", model);
            body.put("prompt", command.getPrompt());
            body.put("image", command.getImage());

            if (command.getMask() != null && !command.getMask().isEmpty()) {
                body.put("mask", command.getMask());
            }
            if (command.getN() != null) {
                body.put("n", command.getN());
            }
            // Apply default size based on model if not specified
            if (command.getSize() != null) {
                body.put("size", command.getSize());
            } else {
                body.put("size", getDefaultSize(model));
            }
            if (command.getResponseFormat() != null) {
                body.put("response_format", command.getResponseFormat());
            }
            if (command.getStrength() != null) {
                body.put("strength", command.getStrength());
            }

            ResponseEntity<String> resp = doPost(getBaseUrl() + "/v1/images/edits", body);
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

            long duration = System.currentTimeMillis() - startTime;

            if (urls.isEmpty() && b64.isEmpty()) {
                String error = "No images returned from KL";
                JsonNode errorNode = json.path("error");
                if (!errorNode.isMissingNode()) {
                    error = "KL error: " + (errorNode.isObject() ?
                            errorNode.path("message").asText(errorNode.toString()) :
                            errorNode.asText(error));
                }
                log.error("KL Image edit error: {}", error);
                return ImageGenResult.builder().error(error).build();
            }

            return ImageGenResult.builder()
                    .id(id)
                    .urls(urls)
                    .base64Data(b64)
                    .build();
        } catch (Exception e) {
            log.error("KL Image edit error", e);
            return ImageGenResult.builder().error(e.getMessage()).build();
        }
    }

    // --- Private helpers ---

    private String getBaseUrl() {
        String baseUrl = settings.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "https://apikl.ai";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
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

    /**
     * Get default size based on model name.
     * - 4k models default to 3840x2160
     * - gpt-image-2.5 (upscale) and other gpt-image-2.5 models default to 2048x2048
     */
    private String getDefaultSize(String model) {
        if (model == null) {
            return DEFAULT_SIZE_2K;
        }
        if (model.endsWith("-4k") || model.contains("-4k-")) {
            return DEFAULT_SIZE_4K;
        }
        return DEFAULT_SIZE_2K;
    }
}
