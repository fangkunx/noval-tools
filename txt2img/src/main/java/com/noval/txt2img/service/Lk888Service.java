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

import java.util.Collections;

@Service
public class Lk888Service {

    @Value("${api.lk888.key}")
    private String apiKey;

    @Value("${api.lk888.base-url:https://api.lk888.ai}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Submit a generate task. Returns task_id immediately.
     */
    public TaskStatus submit(GenerateRequest req) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", defaultIfNull(req.getModel(), "tt-image-2.6"));
        body.put("prompt", req.getPrompt());

        putIfNotNull(body, "aspect_ratio", req.getAspectRatio());
        putIfNotNull(body, "resolution", req.getResolution());
        putIfNotNull(body, "quality", req.getQuality());
        putIfNotNull(body, "complexity", req.getComplexity());
        putIfNotNull(body, "output_format", req.getOutputFormat());
        putIfNotNull(body, "background", req.getBackground());
        putIfNotNull(body, "version", req.getVersion());
        putIfNotNull(body, "size", req.getSize());

        if (req.getReferenceImages() != null && req.getReferenceImages().length > 0) {
            ArrayNode imgs = body.putArray("images");
            for (String img : req.getReferenceImages()) {
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
        TaskStatus status = new TaskStatus();
        status.setTaskId(json.path("data").path("task_id").asLong());
        status.setState("submitted");
        status.setStatus("已提交");
        return status;
    }

    /**
     * Poll task status. Blocks if wait==true, up to 120s.
     */
    public TaskStatus poll(long taskId, boolean wait) throws Exception {
        long deadline = System.currentTimeMillis() + 120_000;

        while (true) {
            String url = baseUrl + "/v1/media/status?task_id=" + taskId;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            JsonNode json = mapper.readTree(resp.getBody());

            TaskStatus status = new TaskStatus();
            status.setTaskId(taskId);
            status.setState(json.path("state").asText());
            status.setStatus(json.path("status").asText());
            status.setProgress(json.path("progress").asText());
            status.setCost(json.path("cost").asDouble());
            status.setIsFinal(json.path("is_final").asBoolean());
            status.setResultType(json.path("result_type").asText());

            JsonNode urls = json.path("result_urls");
            if (urls.isArray() && urls.size() > 0) {
                String[] arr = new String[urls.size()];
                for (int i = 0; i < urls.size(); i++) arr[i] = urls.get(i).asText();
                status.setResultUrls(arr);
                status.setResultUrl(arr[0]);
            } else if (!json.path("result_url").asText().isEmpty()) {
                status.setResultUrl(json.path("result_url").asText());
                status.setResultUrls(new String[]{status.getResultUrl()});
            }

            if (json.path("error") != null && !json.path("error").asText().isEmpty()) {
                status.setError(json.path("error").asText());
            }

            if (!wait || status.getIsFinal() || System.currentTimeMillis() > deadline) {
                if (System.currentTimeMillis() > deadline && !status.getIsFinal()) {
                    status.setError("轮询超时 (120s)，任务可能仍在处理中");
                }
                return status;
            }

            Thread.sleep(Math.min(8000, Math.max(3000, deadline - System.currentTimeMillis())));
        }
    }

    private void putIfNotNull(ObjectNode node, String key, String val) {
        if (val != null && !val.isEmpty() && !"auto".equals(val)) {
            node.put(key, val);
        }
    }

    private String defaultIfNull(String val, String def) {
        return (val == null || val.isEmpty()) ? def : val;
    }
}