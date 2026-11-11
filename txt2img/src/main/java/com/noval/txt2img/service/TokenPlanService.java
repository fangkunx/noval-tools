package com.noval.txt2img.service;

import com.noval.txt2img.dto.GenerateRequest;
import com.noval.txt2img.dto.TaskStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TokenPlanService {

    @Value("${api.tokenplan.key}")
    private String apiKey;

    @Value("${api.tokenplan.base-url:https://token-plan.cn-beijing.maas.aliyuncs.com}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Synchronous generation via Token Plan (百炼) API.
     */
    public TaskStatus generate(GenerateRequest req) throws Exception {
        // Build content array
        ArrayNode content = mapper.createArrayNode();

        // Reference images
        if (req.getReferenceImages() != null) {
            for (String img : req.getReferenceImages()) {
                ObjectNode imgNode = mapper.createObjectNode();
                imgNode.put("image", img);
                content.add(imgNode);
            }
        }

        // Prompt text
        String text = req.getPrompt();
        if (req.getNegativePrompt() != null && !req.getNegativePrompt().isEmpty()) {
            text += " --no " + req.getNegativePrompt();
        }
        ObjectNode textNode = mapper.createObjectNode();
        textNode.put("text", text);
        content.add(textNode);

        // Message
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.set("content", content);

        ArrayNode messages = mapper.createArrayNode();
        messages.add(msg);

        ObjectNode input = mapper.createObjectNode();
        input.set("messages", messages);

        // Parameters
        ObjectNode params = mapper.createObjectNode();
        String size = req.getSize() != null ? req.getSize() : "1024*1024";
        params.put("size", size);
        if (req.getNumImages() > 1) {
            params.put("n", req.getNumImages());
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("model", defaultIfNull(req.getModel(), "qwen-image-3.0-pro"));
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

        TaskStatus status = new TaskStatus();
        status.setState("success");
        status.setStatus("已完成");
        status.setIsFinal(true);
        status.setRequestId(json.path("request_id").asText());

        // Extract image URLs
        JsonNode choices = json.path("output").path("choices");
        List<String> urls = new ArrayList<>();
        if (choices.isArray()) {
            for (JsonNode choice : choices) {
                for (JsonNode item : choice.path("message").path("content")) {
                    if ("image".equals(item.path("type").asText())) {
                        urls.add(item.path("image").asText());
                    }
                }
            }
        }
        String[] urlArr = urls.toArray(new String[0]);
        status.setImageUrls(urlArr);
        status.setResultUrls(urlArr);
        if (urlArr.length > 0) status.setResultUrl(urlArr[0]);

        // Usage
        JsonNode usage = json.path("usage");
        if (!usage.isMissingNode()) {
            status.setUsage(mapper.convertValue(usage, Map.class));
        }

        return status;
    }

    private String defaultIfNull(String val, String def) {
        return (val == null || val.isEmpty()) ? def : val;
    }
}