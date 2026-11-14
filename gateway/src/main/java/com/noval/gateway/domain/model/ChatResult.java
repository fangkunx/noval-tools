package com.noval.gateway.domain.model;

/**
 * Immutable chat response result object.
 */
public class ChatResult {

    private final String id;
    private final String content;
    private final String finishReason;
    private final UsageInfo usage;
    private final String error;
    private final boolean success;

    private ChatResult(Builder builder) {
        this.id = builder.id;
        this.content = builder.content;
        this.finishReason = builder.finishReason;
        this.usage = builder.usage;
        this.error = builder.error;
        this.success = builder.error == null;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public UsageInfo getUsage() {
        return usage;
    }

    public String getError() {
        return error;
    }

    public boolean isSuccess() {
        return success;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String content;
        private String finishReason;
        private UsageInfo usage;
        private String error;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder finishReason(String finishReason) {
            this.finishReason = finishReason;
            return this;
        }

        public Builder usage(UsageInfo usage) {
            this.usage = usage;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public ChatResult build() {
            return new ChatResult(this);
        }
    }

    /**
     * Token usage information.
     */
    public static class UsageInfo {
        private final int promptTokens;
        private final int completionTokens;
        private final int totalTokens;

        public UsageInfo(int promptTokens, int completionTokens, int totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }

        public int getPromptTokens() {
            return promptTokens;
        }

        public int getCompletionTokens() {
            return completionTokens;
        }

        public int getTotalTokens() {
            return totalTokens;
        }
    }
}
