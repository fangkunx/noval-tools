package com.noval.gateway.infrastructure.tracing.langsmith;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * LangSmith 客户端
 * 负责将 trace 数据发送到 LangSmith API
 */
@Component
public class LangSmithClient {

    private static final Logger log = LoggerFactory.getLogger(LangSmithClient.class);

    @Value("${langsmith.api-key:}")
    private String apiKey;

    @Value("${langsmith.endpoint:https://api.smith.langchain.com}")
    private String endpoint;

    @Value("${langsmith.project-name:ai-gateway}")
    private String projectName;

    @Value("${langsmith.enabled:false}")
    private boolean enabled;

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        if (enabled && apiKey != null && !apiKey.isEmpty()) {
            log.info("LangSmith tracing enabled, endpoint: {}, project: {}", endpoint, projectName);
        } else {
            log.info("LangSmith tracing disabled");
        }
    }

    /**
     * 异步发送 Run 到 LangSmith
     */
    @Async
    public void sendRun(LangSmithRun run) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            log.debug("LangSmith disabled, skip sending run: {}", run.getName());
            return;
        }

        try {
            String url = endpoint + "/runs";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);

            // 使用 session_name (project name)
            if (run.getSessionName() == null) {
                run.setSessionName(projectName);
            }

            String json = objectMapper.writeValueAsString(run);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Sent run to LangSmith: {} (trace: {})", run.getName(), run.getTraceId());
            } else {
                log.warn("Failed to send run to LangSmith: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error sending run to LangSmith", e);
        }
    }

    /**
     * 创建并发送一个完整的 Run（开始+结束）
     */
    public LangSmithRun createAndSendRun(UUID traceId, UUID parentRunId, String name,
                                         String runType, Map<String, Object> inputs,
                                         Map<String, Object> outputs, String error,
                                         Map<String, Object> metadata) {
        LangSmithRun run = LangSmithRun.builder()
                .traceId(traceId)
                .parentRunId(parentRunId)
                .name(name)
                .runType(runType)
                .startTime(Instant.now().toString())
                .endTime(Instant.now().toString())
                .inputs(inputs)
                .outputs(outputs)
                .error(error)
                .extra(metadata != null ? metadata : new HashMap<>())
                .sessionName(projectName)
                .build();

        sendRun(run);
        return run;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getProjectName() {
        return projectName;
    }
}
