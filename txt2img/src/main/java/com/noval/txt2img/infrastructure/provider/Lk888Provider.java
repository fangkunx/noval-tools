package com.noval.txt2img.infrastructure.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noval.txt2img.domain.model.*;
import com.noval.txt2img.domain.spi.ImageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class Lk888Provider implements ImageProvider {

    @Value("${api.lk888.key}")
    private String apiKey;

    @Value("${api.lk888.base-url:https://api.lk888.ai}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // --- Model definitions (typed, no Map/String[][]) ---

    private static final List<ModelSpec> MODELS = Collections.unmodifiableList(Arrays.asList(
        new ModelSpec("tt-image-2.6", "TT Image 2.6", BackendType.LK888, Arrays.asList(
            new ModelParam("version", ParamType.SELECT, "flare",
                Arrays.asList("flare", "sunburst")),
            new ModelParam("aspect_ratio", ParamType.SELECT, "1:1",
                Arrays.asList("1:1", "16:9", "9:16", "4:3", "3:4", "3:2", "2:3", "5:4", "4:5", "2:1", "1:2", "21:9", "9:21")),
            new ModelParam("resolution", ParamType.SELECT, "1K",
                Arrays.asList("1K", "2K", "4K")),
            new ModelParam("quality", ParamType.SELECT, "auto",
                Arrays.asList("auto", "low", "medium", "high", "xhigh", "max")),
            new ModelParam("complexity", ParamType.SELECT, "medium",
                Arrays.asList("low", "medium", "high")),
            new ModelParam("output_format", ParamType.SELECT, "png",
                Arrays.asList("png", "jpeg", "webp", "svg", "gif")),
            new ModelParam("background", ParamType.SELECT, "auto",
                Arrays.asList("auto", "opaque", "transparent")),
            new ModelParam("size", ParamType.TEXT, ""),
            new ModelParam("referenceImages", ParamType.IMAGES, "")
        )),
        new ModelSpec("tt-image-2.5", "TT Image 2.5", BackendType.LK888, Arrays.asList(
            new ModelParam("aspect_ratio", ParamType.SELECT, "1:1",
                Arrays.asList("1:1", "16:9", "9:16", "4:3", "3:4", "3:2", "2:3", "5:4", "4:5", "2:1", "1:2", "21:9", "9:21")),
            new ModelParam("resolution", ParamType.SELECT, "1K",
                Arrays.asList("1K", "2K", "4K")),
            new ModelParam("quality", ParamType.SELECT, "auto",
                Arrays.asList("auto", "low", "medium", "high", "xhigh", "max")),
            new ModelParam("output_format", ParamType.SELECT, "png",
                Arrays.asList("png", "jpeg", "webp", "svg", "gif")),
            new ModelParam("background", ParamType.SELECT, "auto",
                Arrays.asList("auto", "opaque", "transparent")),
            new ModelParam("referenceImages", ParamType.IMAGES, "")
        )),
        new ModelSpec("banana-2.1", "Banana 2.1", BackendType.LK888, Arrays.asList(
            new ModelParam("aspect_ratio", ParamType.SELECT, "1:1",
                Arrays.asList("1:1", "16:9", "9:16", "4:3", "3:4")),
            new ModelParam("resolution", ParamType.SELECT, "1K",
                Arrays.asList("1K", "2K", "4K")),
            new ModelParam("quality", ParamType.SELECT, "auto",
                Arrays.asList("auto", "low", "medium", "high")),
            new ModelParam("output_format", ParamType.SELECT, "png",
                Arrays.asList("png", "jpeg", "webp")),
            new ModelParam("referenceImages", ParamType.IMAGES, "")
        )),
        new ModelSpec("gk-image-2.0", "GK Image 2.0", BackendType.LK888, Arrays.asList(
            new ModelParam("aspect_ratio", ParamType.SELECT, "1:1",
                Arrays.asList("1:1", "16:9", "9:16", "4:3", "3:4")),
            new ModelParam("resolution", ParamType.SELECT, "1K",
                Arrays.asList("1K", "2K", "4K")),
            new ModelParam("quality", ParamType.SELECT, "auto",
                Arrays.asList("auto", "low", "medium", "high", "xhigh", "max")),
            new ModelParam("output_format", ParamType.SELECT, "png",
                Arrays.asList("png", "jpeg", "webp")),
            new ModelParam("referenceImages", ParamType.IMAGES, "")
        ))
    ));

    // --- ImageProvider implementation ---

    @Override
    public BackendType getBackend() {
        return BackendType.LK888;
    }

    @Override
    public List<ModelSpec> getSupportedModels() {
        return MODELS;
    }

    @Override
    public boolean supportsAsync() {
        return true;
    }

    @Override
    public ImageGenResult generate(ImageGenCommand cmd) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", defaultIfEmpty(cmd.getModel(), "tt-image-2.6"));
            body.put("prompt", cmd.getPrompt());

            putIfPresent(body, "aspect_ratio", cmd.getAspectRatio());
            putIfPresent(body, "resolution", cmd.getResolution());
            putIfPresent(body, "quality", cmd.getQuality());
            putIfPresent(body, "complexity", cmd.getComplexity());
            putIfPresent(body, "output_format", cmd.getOutputFormat());
            putIfPresent(body, "background", cmd.getBackground());
            putIfPresent(body, "version", cmd.getVersion());
            putIfPresent(body, "size", cmd.getSize());

            if (cmd.getReferenceImages() != null && !cmd.getReferenceImages().isEmpty()) {
                ArrayNode imgs = body.putArray("images");
                for (String img : cmd.getReferenceImages()) {
                    imgs.add(img);
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl + "/v1/media/generate", HttpMethod.POST, entity, String.class);

            JsonNode json = mapper.readTree(resp.getBody());
            String taskId = String.valueOf(json.path("data").path("task_id").asLong());

            return ImageGenResult.builder()
                    .state(ImageGenResult.State.SUBMITTED)
                    .taskId(taskId)
                    .statusText("已提交")
                    .build();
        } catch (Exception e) {
            return ImageGenResult.builder()
                    .state(ImageGenResult.State.FAILED)
                    .error(e.getMessage())
                    .build();
        }
    }

    @Override
    public ImageGenResult poll(String taskId, long timeoutSeconds) {
        // Single-shot poll when timeoutSeconds <= 0 (no wait)
        boolean singleShot = timeoutSeconds <= 0;
        long deadline = System.currentTimeMillis() + Math.max(timeoutSeconds, 1) * 1000;

        while (true) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(apiKey);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> resp = restTemplate.exchange(
                        baseUrl + "/v1/media/status?task_id=" + taskId,
                        HttpMethod.GET, entity, String.class);
                JsonNode json = mapper.readTree(resp.getBody());

                String stateStr = json.path("state").asText();
                boolean isFinal = json.path("is_final").asBoolean();

                ImageGenResult.Builder builder = ImageGenResult.builder()
                        .taskId(taskId)
                        .state(mapState(stateStr))
                        .statusText(json.path("status").asText())
                        .progress(json.path("progress").asText())
                        .cost(json.path("cost").asDouble())
                        .isFinal(isFinal)
                        .resultType(json.path("result_type").asText());

                // Parse result URLs
                List<String> urls = parseResultUrls(json);
                if (!urls.isEmpty()) {
                    builder.resultUrls(urls);
                }

                // Parse error
                JsonNode errNode = json.path("error");
                if (!errNode.isMissingNode() && !errNode.asText().isEmpty()) {
                    builder.error(errNode.asText());
                }

                ImageGenResult result = builder.build();

                // Single shot: return immediately
                if (singleShot) {
                    return result;
                }

                // Result is final or timed out
                if (isFinal || System.currentTimeMillis() >= deadline) {
                    if (!isFinal) {
                        return ImageGenResult.builder()
                                .taskId(taskId)
                                .state(ImageGenResult.State.RUNNING)
                                .error("轮询超时 (" + timeoutSeconds + "s)，任务可能仍在处理中")
                                .isFinal(true)
                                .build();
                    }
                    return result;
                }

                // Still running, wait before next poll
                Thread.sleep(Math.min(8000, Math.max(3000, deadline - System.currentTimeMillis())));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ImageGenResult.builder()
                        .state(ImageGenResult.State.FAILED)
                        .error("轮询被中断")
                        .build();
            } catch (Exception e) {
                return ImageGenResult.builder()
                        .state(ImageGenResult.State.FAILED)
                        .error(e.getMessage())
                        .build();
            }
        }
    }

    // --- Private helpers ---

    private List<String> parseResultUrls(JsonNode json) {
        List<String> urls = new ArrayList<>();
        JsonNode urlsNode = json.path("result_urls");
        if (urlsNode.isArray()) {
            for (JsonNode u : urlsNode) {
                urls.add(u.asText());
            }
        } else {
            String singleUrl = json.path("result_url").asText();
            if (!singleUrl.isEmpty()) {
                urls.add(singleUrl);
            }
        }
        return urls;
    }

    private ImageGenResult.State mapState(String stateStr) {
        if (stateStr == null) return ImageGenResult.State.RUNNING;
        switch (stateStr) {
            case "success": return ImageGenResult.State.SUCCESS;
            case "failed":  return ImageGenResult.State.FAILED;
            case "running": return ImageGenResult.State.RUNNING;
            default:        return ImageGenResult.State.SUBMITTED;
        }
    }

    private void putIfPresent(ObjectNode node, String key, String val) {
        if (val != null && !val.isEmpty() && !"auto".equals(val)) {
            node.put(key, val);
        }
    }

    private String defaultIfEmpty(String val, String def) {
        return (val == null || val.isEmpty()) ? def : val;
    }
}