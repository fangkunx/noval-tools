package com.noval.gateway.interfaces.rest.dto;

import com.noval.gateway.domain.model.ImageGenResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Image composition response DTO.
 */
@Schema(description = "图片合成响应")
public class ComposeResponse {

    @Schema(description = "创建时间戳（Unix 秒）", example = "1700000000")
    private long created;

    @Schema(description = "合成的图像数据列表")
    private List<ImageData> data = new ArrayList<>();

    @Schema(description = "错误信息（如果有）")
    private String error;

    @Schema(description = "任务 ID（用于轮询）")
    private String taskId;

    @Schema(description = "任务状态")
    private String state;

    @Schema(description = "进度百分比")
    private String progress;

    public static ComposeResponse success(ImageGenResult result) {
        ComposeResponse resp = new ComposeResponse();
        resp.created = System.currentTimeMillis() / 1000;
        resp.taskId = result.getId();
        resp.state = "success";
        resp.progress = "100";

        if (result.getUrls() != null) {
            for (String url : result.getUrls()) {
                ImageData img = new ImageData();
                img.url = url;
                resp.data.add(img);
            }
        }

        return resp;
    }

    public static ComposeResponse error(String message) {
        ComposeResponse resp = new ComposeResponse();
        resp.error = message;
        resp.state = "failed";
        return resp;
    }

    public static ComposeResponse processing(String taskId, String progress) {
        ComposeResponse resp = new ComposeResponse();
        resp.taskId = taskId;
        resp.state = "processing";
        resp.progress = progress;
        return resp;
    }

    public long getCreated() {
        return created;
    }

    public List<ImageData> getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getState() {
        return state;
    }

    public String getProgress() {
        return progress;
    }

    @Schema(description = "单个图像数据")
    public static class ImageData {
        @Schema(description = "图像 URL", example = "https://example.com/generated-image.png")
        private String url;

        public String getUrl() {
            return url;
        }
    }
}
