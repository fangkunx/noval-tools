package com.noval.gateway.infrastructure.tracing.langsmith;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * LangSmith Run 对象
 * 符合 LangSmith API 规范：https://docs.smith.langchain.com/reference/api
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LangSmithRun {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("trace_id")
    private UUID traceId;

    @JsonProperty("parent_run_id")
    private UUID parentRunId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("run_type")
    private String runType;  // "llm", "chain", "tool", "retriever"

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    @JsonProperty("inputs")
    private Map<String, Object> inputs;

    @JsonProperty("outputs")
    private Map<String, Object> outputs;

    @JsonProperty("error")
    private String error;

    @JsonProperty("events")
    private Object[] events;

    @JsonProperty("extra")
    private Map<String, Object> extra;

    @JsonProperty("session_id")
    private UUID sessionId;

    @JsonProperty("session_name")
    private String sessionName;

    @JsonProperty("dotted_order")
    private String dottedOrder;

    // Constructor
    public LangSmithRun() {
        this.id = UUID.randomUUID();
        this.startTime = Instant.now().toString();
    }

    // Builder pattern
    public static LangSmithRunBuilder builder() {
        return new LangSmithRunBuilder();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTraceId() {
        return traceId;
    }

    public void setTraceId(UUID traceId) {
        this.traceId = traceId;
    }

    public UUID getParentRunId() {
        return parentRunId;
    }

    public void setParentRunId(UUID parentRunId) {
        this.parentRunId = parentRunId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRunType() {
        return runType;
    }

    public void setRunType(String runType) {
        this.runType = runType;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public Map<String, Object> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object[] getEvents() {
        return events;
    }

    public void setEvents(Object[] events) {
        this.events = events;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getDottedOrder() {
        return dottedOrder;
    }

    public void setDottedOrder(String dottedOrder) {
        this.dottedOrder = dottedOrder;
    }

    // Builder class
    public static class LangSmithRunBuilder {
        private LangSmithRun run;

        public LangSmithRunBuilder() {
            this.run = new LangSmithRun();
        }

        public LangSmithRunBuilder id(UUID id) {
            run.setId(id);
            return this;
        }

        public LangSmithRunBuilder traceId(UUID traceId) {
            run.setTraceId(traceId);
            return this;
        }

        public LangSmithRunBuilder parentRunId(UUID parentRunId) {
            run.setParentRunId(parentRunId);
            return this;
        }

        public LangSmithRunBuilder name(String name) {
            run.setName(name);
            return this;
        }

        public LangSmithRunBuilder runType(String runType) {
            run.setRunType(runType);
            return this;
        }

        public LangSmithRunBuilder startTime(String startTime) {
            run.setStartTime(startTime);
            return this;
        }

        public LangSmithRunBuilder endTime(String endTime) {
            run.setEndTime(endTime);
            return this;
        }

        public LangSmithRunBuilder inputs(Map<String, Object> inputs) {
            run.setInputs(inputs);
            return this;
        }

        public LangSmithRunBuilder outputs(Map<String, Object> outputs) {
            run.setOutputs(outputs);
            return this;
        }

        public LangSmithRunBuilder error(String error) {
            run.setError(error);
            return this;
        }

        public LangSmithRunBuilder extra(Map<String, Object> extra) {
            run.setExtra(extra);
            return this;
        }

        public LangSmithRunBuilder sessionId(UUID sessionId) {
            run.setSessionId(sessionId);
            return this;
        }

        public LangSmithRunBuilder sessionName(String sessionName) {
            run.setSessionName(sessionName);
            return this;
        }

        public LangSmithRunBuilder dottedOrder(String dottedOrder) {
            run.setDottedOrder(dottedOrder);
            return this;
        }

        public LangSmithRun build() {
            return run;
        }
    }
}
