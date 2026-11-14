package com.noval.txt2img.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * Domain result of an image generation request (immutable, built via Builder).
 * Unified result object for all backends.
 */
public class ImageGenResult {

    public enum State {
        SUBMITTED, RUNNING, SUCCESS, FAILED
    }

    private final State state;
    private final String taskId;
    private final String statusText;
    private final String progress;
    private final List<String> resultUrls;
    private final String error;
    private final Double cost;
    private final boolean isFinal;
    private final String resultType;
    private final String requestId;
    private final UsageInfo usage;

    private ImageGenResult(Builder builder) {
        this.state = builder.state;
        this.taskId = builder.taskId;
        this.statusText = builder.statusText;
        this.progress = builder.progress;
        this.resultUrls = builder.resultUrls == null
                ? Collections.emptyList() : Collections.unmodifiableList(builder.resultUrls);
        this.error = builder.error;
        this.cost = builder.cost;
        this.isFinal = builder.isFinal;
        this.resultType = builder.resultType;
        this.requestId = builder.requestId;
        this.usage = builder.usage;
    }

    public static Builder builder() { return new Builder(); }

    public State getState() { return state; }
    public String getTaskId() { return taskId; }
    public String getStatusText() { return statusText; }
    public String getProgress() { return progress; }
    public List<String> getResultUrls() { return resultUrls; }
    public String getError() { return error; }
    public Double getCost() { return cost; }
    public boolean isFinal() { return isFinal; }
    public String getResultType() { return resultType; }
    public String getRequestId() { return requestId; }
    public UsageInfo getUsage() { return usage; }

    /**
     * Token/credit usage info (typed, not Map).
     */
    public static class UsageInfo {
        private final Integer totalTokens;
        private final Integer promptTokens;
        private final Integer completionTokens;

        public UsageInfo(Integer totalTokens, Integer promptTokens, Integer completionTokens) {
            this.totalTokens = totalTokens;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() { return totalTokens; }
        public Integer getPromptTokens() { return promptTokens; }
        public Integer getCompletionTokens() { return completionTokens; }
    }

    public static class Builder {
        private State state;
        private String taskId;
        private String statusText;
        private String progress;
        private List<String> resultUrls;
        private String error;
        private Double cost;
        private boolean isFinal;
        private String resultType;
        private String requestId;
        private UsageInfo usage;

        public Builder state(State state) { this.state = state; return this; }
        public Builder taskId(String taskId) { this.taskId = taskId; return this; }
        public Builder statusText(String statusText) { this.statusText = statusText; return this; }
        public Builder progress(String progress) { this.progress = progress; return this; }
        public Builder resultUrls(List<String> resultUrls) { this.resultUrls = resultUrls; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder cost(Double cost) { this.cost = cost; return this; }
        public Builder isFinal(boolean isFinal) { this.isFinal = isFinal; return this; }
        public Builder resultType(String resultType) { this.resultType = resultType; return this; }
        public Builder requestId(String requestId) { this.requestId = requestId; return this; }
        public Builder usage(UsageInfo usage) { this.usage = usage; return this; }

        public ImageGenResult build() { return new ImageGenResult(this); }
    }
}