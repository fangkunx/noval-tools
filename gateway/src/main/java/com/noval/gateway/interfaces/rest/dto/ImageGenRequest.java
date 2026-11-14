package com.noval.gateway.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OpenAI-compatible image generation request.
 */
@Schema(description = "图像生成请求 - 兼容 OpenAI 协议")
public class ImageGenRequest {

    @Schema(description = "模型 ID，例如 dall-e-3", example = "dall-e-3")
    private String model;

    @Schema(description = "图像描述提示词", example = "A cute cat playing with yarn")
    private String prompt;

    @Schema(description = "生成图片数量", example = "1")
    private Integer n = 1;

    @Schema(description = "图片尺寸", example = "1024x1024")
    private String size = "1024x1024";

    @Schema(description = "响应格式：url 或 b64_json", example = "url")
    private String response_format = "url";

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Integer getN() {
        return n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getResponse_format() {
        return response_format;
    }

    public void setResponse_format(String response_format) {
        this.response_format = response_format;
    }
}
