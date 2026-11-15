package com.noval.gateway.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI-compatible image generation response.
 */
@Schema(description = "图像生成响应 - 兼容 OpenAI 协议")
public class ImageGenResponse {

    @Schema(description = "创建时间戳（Unix 秒）", example = "1700000000")
    private long created;

    @Schema(description = "生成的图像数据列表")
    private List<ImageData> data = new ArrayList<>();

    @Schema(description = "错误信息（如果有）")
    private String error;

    public static ImageGenResponse success(String id, List<String> urls, List<String> b64Data) {
        ImageGenResponse resp = new ImageGenResponse();
        resp.created = System.currentTimeMillis() / 1000;

        for (int i = 0; i < urls.size(); i++) {
            ImageData img = new ImageData();
            img.url = urls.get(i);
            resp.data.add(img);
        }
        for (int i = 0; i < b64Data.size(); i++) {
            ImageData img = new ImageData();
            img.b64_json = b64Data.get(i);
            resp.data.add(img);
        }

        return resp;
    }

    public static ImageGenResponse error(String message) {
        ImageGenResponse resp = new ImageGenResponse();
        resp.error = message;
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

    @Schema(description = "单个图像数据")
    public static class ImageData {
        @Schema(description = "图像 URL（当 response_format=url 时）", example = "https://example.com/generated-image.png")
        private String url;

        @Schema(description = "Base64 编码的图像数据（当 response_format=b64_json 时）")
        private String b64_json;

        @Schema(description = "修订后的提示词（部分模型返回）")
        private String revised_prompt;

        public String getUrl() {
            return url;
        }

        public String getB64_json() {
            return b64_json;
        }

        public String getRevised_prompt() {
            return revised_prompt;
        }
    }
}
