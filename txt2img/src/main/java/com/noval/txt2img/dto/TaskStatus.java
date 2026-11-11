package com.noval.txt2img.dto;

import java.util.Map;

public class TaskStatus {
    private long taskId;
    private String state;        // success | running | failed
    private String status;       // 已完成 | 处理中 | 任务失败
    private String progress;
    private String resultUrl;
    private String[] resultUrls;
    private String error;
    private double cost;
    private boolean isFinal;
    private String resultType;

    // For tokenplan sync response
    private String[] imageUrls;
    private String requestId;
    private Map<String, Object> usage;

    public long getTaskId() { return taskId; }
    public void setTaskId(long taskId) { this.taskId = taskId; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProgress() { return progress; }
    public void setProgress(String progress) { this.progress = progress; }
    public String getResultUrl() { return resultUrl; }
    public void setResultUrl(String resultUrl) { this.resultUrl = resultUrl; }
    public String[] getResultUrls() { return resultUrls; }
    public void setResultUrls(String[] resultUrls) { this.resultUrls = resultUrls; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public boolean getIsFinal() { return isFinal; }
    public void setIsFinal(boolean isFinal) { this.isFinal = isFinal; }
    public String getResultType() { return resultType; }
    public void setResultType(String resultType) { this.resultType = resultType; }
    public String[] getImageUrls() { return imageUrls; }
    public void setImageUrls(String[] imageUrls) { this.imageUrls = imageUrls; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Map<String, Object> getUsage() { return usage; }
    public void setUsage(Map<String, Object> usage) { this.usage = usage; }
}