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
public class TokenPlanProvider implements ImageProvider {

    @Value("${api.tokenplan.key}")
    private String apiKey;

    @Value("${api.tokenplan.base-url:https://token-plan.cn-beijing.maas.aliyuncs.com}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // --- Model definitions ---

    private static final List<ModelSpec> MODELS = Collections.unmodifiableList(Arrays.asList(
        new ModelSpec("qwen-image-3.0-pro", "通义万相 3.0 Pro", BackendType.TOKENPLAN, Arrays.asList(
            new ModelParam("size", ParamType.SELECT, "1024*1024",
                Arrays.asList("1024*1024", "1280*720", "720*1280", "1024*768", "768*1024", "1792*768")),
            new ModelParam("numImages", ParamType.NUMBER, "1"),
            new ModelParam("negativePrompt", ParamType.TEXT, ""),
            new ModelParam("referenceImages", ParamType.IMAGES, "")
        )),
        new ModelSpec("qwen-image-2.0-pro", "通义万相 2.0 Pro", BackendType.TOKENPLAN, Arrays.asList(
            new ModelParam("size", ParamType.SELECT, "1024*1024",
                Arrays.asList("1024*1024", "1280*720", "720*1280", "1024*768", "768*1024", "1792*768")),
            new ModelParam("numImages", ParamType.NUMBER, "1"),
            new ModelParam("negativePrompt", ParamType.TEXT, ""),
            new ModelParam("referenceImages", ParamType.IMAGES, "")
        )),
        new ModelSpec("wan2.7-image", "万相 2.7", BackendType.TOKENPLAN, Arrays.asList(
            new ModelParam("size", ParamType.SELECT, "1024*1024",
                Arrays.asList("1024*1024", "1280*720", "720*1280", "1024*768", "768*1024", "1792*768")),
            new ModelParam("numImages", ParamType.NUMBER, "1"),
            new ModelParam("negativePrompt", ParamType.TEXT, ""),
            new ModelParam("referenceImages", ParamType.IMAGES, "")
        ))
    ));

    // --- ImageProvider implementation ---

    @Override
    public BackendType getBackend() {
        return BackendType.TOKENPLAN;
    }

    @Override
    public List<ModelSpec> getSupportedModels() {
        return MODELS;
    }

    @Override
    public boolean supportsAsync() {
        return false; // TokenPlan is synchronous
    }

    @Override
    public ImageGenResult generate(ImageGenCommand cmd) {
        try {
            // Build content array: [reference images..., text prompt]
            ArrayNode content = mapper.createArrayNode();

            if (cmd.getReferenceImages() != null) {
                for (String img : cmd.getReferenceImages()) {
                    ObjectNode imgNode = mapper.createObjectNode();
                    imgNode.put("image", img);
                    content.add(imgNode);
                }
            }

            // Combine prompt with negative prompt
            String text = cmd.getPrompt();
            if (cmd.getNegativePrompt() != null && !cmd.getNegativePrompt().isEmpty()) {
                text += " --no " + cmd.getNegativePrompt();
            }
            ObjectNode textNode = mapper.createObjectNode();
            textNode.put("text", text);
            content.add(textNode);

            // Build message
            ObjectNode msg = mapper.createObjectNode();
            msg.put("role", "user");
            msg.set("content", content);
            ArrayNode messages = mapper.createArrayNode();
            messages.add(msg);

            ObjectNode input = mapper.createObjectNode();
            input.set("messages", messages);

            // Parameters
            ObjectNode params = mapper.createObjectNode();
            String size = cmd.getSize() != null ? cmd.getSize() : "1024*1024";
            params.put("size", size);
            if (cmd.getNumImages() != null && cmd.getNumImages() > 1) {
                params.put("n", cmd.getNumImages());
            }

            // Request body
            ObjectNode body = mapper.createObjectNode();
            body.put("model", defaultIfEmpty(cmd.getModel(), "qwen-image-3.0-pro"));
            body.set("input", input);
            body.set("parameters", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl + "/api/v1/services/aigc/multimodal-generation/generation",
                    HttpMethod.POST, entity, String.class);

            JsonNode json = mapper.readTree(resp.getBody());

            // Extract image URLs
            List<String> urls = new ArrayList<>();
            JsonNode choices = json.path("output").path("choices");
            if (choices.isArray()) {
                for (JsonNode choice : choices) {
                    for (JsonNode item : choice.path("message").path("content")) {
                        if ("image".equals(item.path("type").asText())) {
                            urls.add(item.path("image").asText());
                        }
                    }
                }
            }

            // Parse usage info
            ImageGenResult.UsageInfo usage = null;
            JsonNode usageNode = json.path("usage");
            if (!usageNode.isMissingNode()) {
                usage = new ImageGenResult.UsageInfo(
                    usageNode.path("total_tokens").isMissingNode() ? null : usageNode.path("total_tokens").asInt(),
                    usageNode.path("prompt_tokens").isMissingNode() ? null : usageNode.path("prompt_tokens").asInt(),
                    usageNode.path("completion_tokens").isMissingNode() ? null : usageNode.path("completion_tokens").asInt()
                );
            }

            return ImageGenResult.builder()
                    .state(ImageGenResult.State.SUCCESS)
                    .statusText("已完成")
                    .isFinal(true)
                    .requestId(json.path("request_id").asText())
                    .resultUrls(urls)
                    .usage(usage)
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
        return ImageGenResult.builder()
                .state(ImageGenResult.State.FAILED)
                .error("TokenPlan does not support async polling; use generate() for synchronous results")
                .build();
    }

    private String defaultIfEmpty(String val, String def) {
        return (val == null || val.isEmpty()) ? def : val;
    }
}