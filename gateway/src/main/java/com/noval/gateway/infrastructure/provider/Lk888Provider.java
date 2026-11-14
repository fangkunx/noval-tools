package com.noval.gateway.infrastructure.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noval.gateway.domain.model.*;
import com.noval.gateway.domain.spi.GatewayProvider;
import com.noval.gateway.infrastructure.config.ProviderConfig;
import com.noval.gateway.infrastructure.logging.ProviderRequestLogger;
import com.noval.gateway.infrastructure.tracing.langsmith.LangSmithRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Consumer;

/**
 * LK888 图像生成 API Provider
 * 支持模型：tt-image-2.5, tt-image-2.6, banana-2.1, gk-image-2.0
 * 异步模式：提交任务后返回 task_id，需要轮询获取结果
 */
public class Lk888Provider implements GatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(Lk888Provider.class);
    private static final String PROVIDER_NAME = "LK888";

    private final ProviderConfig.ProviderSettings settings;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final ProviderRequestLogger requestLogger;

    private static final List<ModelSpec> MODELS = Collections.unmodifiableList(Arrays.asList(
            new ModelSpec("tt-image-2.5", "TT Image 2.5", ProviderType.LK888,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("tt-image-2.6", "TT Image 2.6", ProviderType.LK888,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("banana-2.1", "Banana 2.1", ProviderType.LK888,
                    EnumSet.of(Capability.IMAGE_GEN)),
            new ModelSpec("gk-image-2.0", "GK Image 2.0", ProviderType.LK888,
                    EnumSet.of(Capability.IMAGE_GEN))
    ));

    public Lk888Provider(ProviderConfig.ProviderSettings settings, ProviderRequestLogger requestLogger) {
        this.settings = settings;
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
        this.requestLogger = requestLogger;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.LK888;
    }

    @Override
    public List<ModelSpec> getSupportedModels() {
        return MODELS;
    }

    @Override
    public Set<Capability> getSupportedCapabilities() {
        return EnumSet.of(Capability.IMAGE_GEN);
    }

    @Override
    public boolean supportsModel(String modelId) {
        return MODELS.stream().anyMatch(m -> m.getId().equals(modelId));
    }

    @Override
    public ChatResult chat(ChatCommand command, String modelName) {
        return ChatResult.builder()
                .error("LK888 does not support chat")
                .build();
    }

    @Override
    public void chatStream(ChatCommand command, String modelName,
                           Consumer<ChatStreamChunk> onChunk, Consumer<Throwable> onError) {
        onError.accept(new UnsupportedOperationException("LK888 does not support streaming"));
    }

    @Override
    public ImageGenResult imageGen(ImageGenCommand command, String modelName) {
        long startTime = System.currentTimeMillis();
        LangSmithRun run = requestLogger.logImageGenRequestStart(PROVIDER_NAME, modelName, command);

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", defaultIfEmpty(modelName, "tt-image-2.5"));
            body.put("prompt", command.getPrompt());

            // LK888 API 参数
            putIfPresent(body, "aspect_ratio", "1:1"); // 默认 1:1
            putIfPresent(body, "resolution", "1K"); // 默认 1K
            putIfPresent(body, "quality", "auto");
            putIfPresent(body, "output_format", "png");
            putIfPresent(body, "background", "auto");

            HttpHeaders headers = buildHeaders();
            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    settings.getBaseUrl() + "/v1/media/generate",
                    HttpMethod.POST,
                    entity,
                    String.class);

            JsonNode json = mapper.readTree(resp.getBody());
            String taskId = json.path("data").path("task_id").asText();

            if (taskId == null || taskId.isEmpty()) {
                long duration = System.currentTimeMillis() - startTime;
                String error = "Failed to get task_id from LK888";
                requestLogger.logImageGenRequestError(PROVIDER_NAME, modelName, duration, error, run);
                return ImageGenResult.builder()
                        .error(error)
                        .build();
            }

            // 轮询获取结果（最多等待 60 秒）
            ImageGenResult result = pollTask(taskId, 60);
            long duration = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                requestLogger.logImageGenRequestSuccess(PROVIDER_NAME, modelName, duration, result.getUrls().size(), run);
            } else {
                requestLogger.logImageGenRequestError(PROVIDER_NAME, modelName, duration, result.getError(), run);
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String error = "LK888 image generation failed: " + e.getMessage();
            requestLogger.logImageGenRequestError(PROVIDER_NAME, modelName, duration, error, run);
            log.error("LK888 image generation error", e);
            return ImageGenResult.builder()
                    .error(error)
                    .build();
        }
    }

    /**
     * 轮询任务状态
     */
    private ImageGenResult pollTask(String taskId, int maxWaitSeconds) {
        long deadline = System.currentTimeMillis() + maxWaitSeconds * 1000L;

        while (System.currentTimeMillis() < deadline) {
            try {
                HttpHeaders headers = buildHeaders();
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> resp = restTemplate.exchange(
                        settings.getBaseUrl() + "/v1/media/status?task_id=" + taskId,
                        HttpMethod.GET,
                        entity,
                        String.class);

                JsonNode json = mapper.readTree(resp.getBody());
                String state = json.path("state").asText();
                boolean isFinal = json.path("is_final").asBoolean();

                if ("success".equals(state)) {
                    // 获取结果 URL
                    List<String> urls = new ArrayList<>();
                    JsonNode resultUrl = json.path("result_url");
                    if (!resultUrl.isMissingNode()) {
                        urls.add(resultUrl.asText());
                    }
                    JsonNode resultUrls = json.path("result_urls");
                    if (resultUrls.isArray()) {
                        for (JsonNode url : resultUrls) {
                            urls.add(url.asText());
                        }
                    }

                    return ImageGenResult.builder()
                            .id(taskId)
                            .urls(urls)
                            .build();

                } else if ("failed".equals(state)) {
                    String error = json.path("error").asText("Unknown error");
                    return ImageGenResult.builder()
                            .error("LK888 task failed: " + error)
                            .build();
                }

                // 如果任务已完成但不是 success，返回错误
                if (isFinal) {
                    return ImageGenResult.builder()
                            .error("LK888 task ended with unexpected state: " + state)
                            .build();
                }

                // 等待 3 秒后重试
                Thread.sleep(3000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ImageGenResult.builder()
                        .error("Task polling interrupted")
                        .build();
            } catch (Exception e) {
                log.error("Error polling task status", e);
                return ImageGenResult.builder()
                        .error("Error polling task: " + e.getMessage())
                        .build();
            }
        }

        return ImageGenResult.builder()
                .error("Task polling timeout after " + maxWaitSeconds + " seconds")
                .build();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(settings.getApiKey());
        return headers;
    }

    private String defaultIfEmpty(String val, String def) {
        return (val == null || val.isEmpty()) ? def : val;
    }

    private void putIfPresent(ObjectNode node, String key, String val) {
        if (val != null && !val.isEmpty()) {
            node.put(key, val);
        }
    }
}
