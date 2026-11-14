package com.noval.gateway.infrastructure.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noval.gateway.domain.model.ChatCommand;
import com.noval.gateway.domain.model.ImageGenCommand;
import com.noval.gateway.infrastructure.tracing.langsmith.LangSmithClient;
import com.noval.gateway.infrastructure.tracing.langsmith.LangSmithRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provider 请求日志记录器
 * 同时支持本地日志和 LangSmith trace 上报
 */
@Component
public class ProviderRequestLogger {

    private static final Logger log = LoggerFactory.getLogger("provider.request");

    private final ObjectMapper objectMapper;
    private boolean logRequestBody;
    private boolean logResponseBody;

    @Autowired(required = false)
    private LangSmithClient langSmithClient;

    public ProviderRequestLogger(ObjectMapper objectMapper,
                                  @Value("${gateway.logging.request-body:true}") boolean logRequestBody,
                                  @Value("${gateway.logging.response-body:true}") boolean logResponseBody) {
        this.objectMapper = objectMapper;
        this.logRequestBody = logRequestBody;
        this.logResponseBody = logResponseBody;
    }

    /**
     * 记录聊天请求开始
     */
    public LangSmithRun logChatRequestStart(String provider, String model, ChatCommand command) {
        String traceId = TraceContext.getTraceId();
        String parentRunId = TraceContext.getParentRunId();
        String reqJson = null;
        // 本地日志
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Provider Request Start] ");
            sb.append("provider=").append(provider);
            sb.append(", model=").append(model);
            sb.append(", messageCount=").append(command.getMessages().size());
            sb.append(", stream=").append(command.isStream());
            sb.append(", traceId=").append(traceId);

            if (logRequestBody) {
                try {
                    String json = objectMapper.writeValueAsString(command);
                    reqJson = json;
                    sb.append(", body=").append(json);
                } catch (Exception e) {
                    sb.append(", body=[serialize error]");
                }
            }

            log.info(sb.toString());
        }

        // LangSmith trace
        LangSmithRun run = null;
        if (langSmithClient != null && langSmithClient.isEnabled()) {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("provider", provider);
            inputs.put("model", model);
            inputs.put("message_count", command.getMessages().size());
            inputs.put("reqJson", reqJson);
            inputs.put("stream", command.isStream());

            UUID traceUuid = traceId != null ? UUID.fromString(traceId) : UUID.randomUUID();
            UUID runId;

            // 如果是根 run（没有 parent），使用 trace_id 作为 run id
            if (parentRunId == null) {
                runId = traceUuid;
            } else {
                runId = UUID.fromString(TraceContext.newSpanId());
            }

            // 生成 dotted_order: timestamp (YYYYMMDDTHHMMSSuuuuuuZ) + run_id (无连字符)
            // LangSmith 要求时间部分最多12位数字（HHMMSSuuuuuu）
            java.time.ZonedDateTime now = Instant.now().atZone(java.time.ZoneOffset.UTC);
            String timestamp = String.format("%04d%02d%02dT%02d%02d%02d%06dZ",
                    now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                    now.getHour(), now.getMinute(), now.getSecond(),
                    now.getNano() / 1000); // 纳秒转微秒
            String dottedOrder = timestamp + runId.toString().replace("-", "");

            run = LangSmithRun.builder()
                    .id(runId)
                    .traceId(traceUuid)
                    .parentRunId(parentRunId != null ? UUID.fromString(parentRunId) : null)
                    .name(provider + " Chat")
                    .runType("llm")
                    .startTime(Instant.now().toString())
                    .inputs(inputs)
                    .sessionName(langSmithClient.getProjectName())
                    .dottedOrder(dottedOrder)
                    .build();

            // Don't send yet - will send complete run after request finishes
        }

        return run;
    }

    /**
     * 记录聊天请求成功
     */
    public void logChatRequestSuccess(String provider, String model, long duration, String content, LangSmithRun run) {
        String traceId = TraceContext.getTraceId();

        // 本地日志
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Provider Request Success] ");
            sb.append("provider=").append(provider);
            sb.append(", model=").append(model);
            sb.append(", duration=").append(duration).append("ms");
            sb.append(", traceId=").append(traceId);

            if (logResponseBody) {
                String preview = content != null && content.length() > 200
                        ? content.substring(0, 200) + "..."
                        : content;
                sb.append(", content=").append(preview);
            }

            log.info(sb.toString());
        }

        // LangSmith trace
        if (langSmithClient != null && langSmithClient.isEnabled() && run != null) {
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("content", content);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("duration_ms", duration);
            metadata.put("provider", provider);
            metadata.put("model", model);

            run.setEndTime(Instant.now().toString());
            run.setOutputs(outputs);
            run.setExtra(metadata);

            langSmithClient.sendRun(run);
        }
    }

    /**
     * 记录聊天请求失败
     */
    public void logChatRequestError(String provider, String model, long duration, String error, LangSmithRun run) {
        String traceId = TraceContext.getTraceId();

        // 本地日志
        StringBuilder sb = new StringBuilder();
        sb.append("[Provider Request Error] ");
        sb.append("provider=").append(provider);
        sb.append(", model=").append(model);
        sb.append(", duration=").append(duration).append("ms");
        sb.append(", traceId=").append(traceId);
        sb.append(", error=").append(error);

        log.error(sb.toString());

        // LangSmith trace
        if (langSmithClient != null && langSmithClient.isEnabled() && run != null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("duration_ms", duration);
            metadata.put("provider", provider);
            metadata.put("model", model);

            run.setEndTime(Instant.now().toString());
            run.setError(error);
            run.setExtra(metadata);

            // Send the complete run with error
            langSmithClient.sendRun(run);
        }
    }

    /**
     * 记录图像生成请求开始
     */
    public LangSmithRun logImageGenRequestStart(String provider, String model, ImageGenCommand command) {
        String traceId = TraceContext.getTraceId();
        String parentRunId = TraceContext.getParentRunId();

        // 本地日志
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Provider ImageGen Start] ");
            sb.append("provider=").append(provider);
            sb.append(", model=").append(model);
            sb.append(", prompt=").append(command.getPrompt());
            sb.append(", traceId=").append(traceId);

            if (logRequestBody) {
                try {
                    String json = objectMapper.writeValueAsString(command);
                    sb.append(", body=").append(json);
                } catch (Exception e) {
                    sb.append(", body=[serialize error]");
                }
            }

            log.info(sb.toString());
        }

        // LangSmith trace
        LangSmithRun run = null;
        if (langSmithClient != null && langSmithClient.isEnabled()) {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("provider", provider);
            inputs.put("model", model);
            inputs.put("prompt", command.getPrompt());

            UUID traceUuid = traceId != null ? UUID.fromString(traceId) : UUID.randomUUID();
            UUID runId;

            // 如果是根 run（没有 parent），使用 trace_id 作为 run id
            if (parentRunId == null) {
                runId = traceUuid;
            } else {
                runId = UUID.fromString(TraceContext.newSpanId());
            }

            // 生成 dotted_order: timestamp (YYYYMMDDTHHMMSSuuuuuuZ) + run_id (无连字符)
            java.time.ZonedDateTime now = Instant.now().atZone(java.time.ZoneOffset.UTC);
            String timestamp = String.format("%04d%02d%02dT%02d%02d%02d%06dZ",
                    now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                    now.getHour(), now.getMinute(), now.getSecond(),
                    now.getNano() / 1000); // 纳秒转微秒
            String dottedOrder = timestamp + runId.toString().replace("-", "");

            run = LangSmithRun.builder()
                    .id(runId)
                    .traceId(traceUuid)
                    .parentRunId(parentRunId != null ? UUID.fromString(parentRunId) : null)
                    .name(provider + " ImageGen")
                    .runType("tool")
                    .startTime(Instant.now().toString())
                    .inputs(inputs)
                    .sessionName(langSmithClient.getProjectName())
                    .dottedOrder(dottedOrder)
                    .build();

            // Don't send yet - will send complete run after request finishes
        }

        return run;
    }

    /**
     * 记录图像生成请求成功
     */
    public void logImageGenRequestSuccess(String provider, String model, long duration, int imageCount, LangSmithRun run) {
        String traceId = TraceContext.getTraceId();

        // 本地日志
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Provider ImageGen Success] ");
            sb.append("provider=").append(provider);
            sb.append(", model=").append(model);
            sb.append(", duration=").append(duration).append("ms");
            sb.append(", imageCount=").append(imageCount);
            sb.append(", traceId=").append(traceId);

            log.info(sb.toString());
        }

        // LangSmith trace
        if (langSmithClient != null && langSmithClient.isEnabled() && run != null) {
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("image_count", imageCount);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("duration_ms", duration);
            metadata.put("provider", provider);
            metadata.put("model", model);

            run.setEndTime(Instant.now().toString());
            run.setOutputs(outputs);
            run.setExtra(metadata);

            langSmithClient.sendRun(run);
        }
    }

    /**
     * 记录图像生成请求失败
     */
    public void logImageGenRequestError(String provider, String model, long duration, String error, LangSmithRun run) {
        String traceId = TraceContext.getTraceId();

        // 本地日志
        StringBuilder sb = new StringBuilder();
        sb.append("[Provider ImageGen Error] ");
        sb.append("provider=").append(provider);
        sb.append(", model=").append(model);
        sb.append(", duration=").append(duration).append("ms");
        sb.append(", traceId=").append(traceId);
        sb.append(", error=").append(error);

        log.error(sb.toString());

        // LangSmith trace
        if (langSmithClient != null && langSmithClient.isEnabled() && run != null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("duration_ms", duration);
            metadata.put("provider", provider);
            metadata.put("model", model);

            run.setEndTime(Instant.now().toString());
            run.setError(error);
            run.setExtra(metadata);

            langSmithClient.sendRun(run);
        }
    }
}

