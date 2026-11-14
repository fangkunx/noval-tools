package com.noval.txt2img.interfaces.rest.dto;

import com.noval.txt2img.domain.model.ImageGenResult;

import java.util.List;

/**
 * REST response DTO for task status / generation result.
 * Assembled from domain ImageGenResult.
 */
public class TaskStatusResponse {

    private String taskId;
    private String state;
    private String status;
    private String progress;
    private String resultUrl;
    private List<String> resultUrls;
    private String error;
    private Double cost;
    private boolean isFinal;
    private String resultType;
    private String requestId;
    private UsageResponse usage;

    public static TaskStatusResponse from(ImageGenResult result) {
        TaskStatusResponse r = new TaskStatusResponse();
        r.taskId = result.getTaskId();
        r.state = result.getState() != null ? result.getState().name().toLowerCase() : null;
        r.status = result.getStatusText();
        r.progress = result.getProgress();
        r.error = result.getError();
        r.cost = result.getCost();
        r.isFinal = result.isFinal();
        r.resultType = result.getResultType();
        r.requestId = result.getRequestId();

        List<String> urls = result.getResultUrls();
        if (urls != null && !urls.isEmpty()) {
            r.resultUrls = urls;
            r.resultUrl = urls.get(0);
        }

        if (result.getUsage() != null) {
            r.usage = new UsageResponse(
                result.getUsage().getTotalTokens(),
                result.getUsage().getPromptTokens(),
                result.getUsage().getCompletionTokens()
            );
        }

        return r;
    }

    public String getTaskId() { return taskId; }
    public String getState() { return state; }
    public String getStatus() { return status; }
    public String getProgress() { return progress; }
    public String getResultUrl() { return resultUrl; }
    public List<String> getResultUrls() { return resultUrls; }
    public String getError() { return error; }
    public Double getCost() { return cost; }
    public boolean getIsFinal() { return isFinal; }
    public String getResultType() { return resultType; }
    public String getRequestId() { return requestId; }
    public UsageResponse getUsage() { return usage; }

    /**
     * Typed usage info in REST response.
     */
    public static class UsageResponse {
        private final Integer totalTokens;
        private final Integer promptTokens;
        private final Integer completionTokens;

        public UsageResponse(Integer totalTokens, Integer promptTokens, Integer completionTokens) {
            this.totalTokens = totalTokens;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() { return totalTokens; }
        public Integer getPromptTokens() { return promptTokens; }
        public Integer getCompletionTokens() { return completionTokens; }
    }
}