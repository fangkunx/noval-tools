package com.noval.gateway.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OpenAI-compatible image edit request (image-to-image).
 */
@Schema(description = "图像编辑请求 - 图生图\n\n" +
        "**支持的模型:**\n" +
        "- gpt-image-1\n" +
        "- flux-pro, flux-dev, flux-schnell")
public class ImageEditRequest {

    @Schema(description = "模型 ID", example = "gpt-image-1")
    private String model;

    @Schema(description = "编辑提示词", example = "Add a sunset background to the image")
    private String prompt;

    @Schema(description = "输入图片 (Base64 编码或 URL)", example = "https://example.com/image.png")
    private String image;

    @Schema(description = "遮罩图片 (Base64 编码或 URL，用于局部重绘)", example = "https://example.com/mask.png")
    private String mask;

    @Schema(description = "生成图片数量", example = "1")
    private Integer n = 1;

    @Schema(description = "图片尺寸 (不指定时根据模型使用默认值)", example = "1024x1024")
    private String size;  // 默认为 null，由 Provider 根据模型设置默认值

    @Schema(description = "响应格式：url 或 b64_json", example = "url")
    private String response_format = "url";

    @Schema(description = "编辑强度 (0.0-1.0，越高变化越大)", example = "0.75")
    private Float strength = 0.75f;

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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getMask() {
        return mask;
    }

    public void setMask(String mask) {
        this.mask = mask;
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

    public Float getStrength() {
        return strength;
    }

    public void setStrength(Float strength) {
        this.strength = strength;
    }
}
