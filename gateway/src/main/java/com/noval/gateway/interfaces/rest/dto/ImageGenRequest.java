package com.noval.gateway.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OpenAI-compatible image generation request.
 */
@Schema(description = "图像生成请求 - 兼容 OpenAI 协议\n\n" +
        "**支持的模型:**\n" +
        "- OpenAI: dall-e-3, dall-e-2\n" +
        "- LK888: tt-image-2.5, tt-image-2.6, banana-2.1, gk-image-2.0\n" +
        "- **Grsai Nano Banana 系列**:\n" +
        "  - nano-banana, nano-banana-fast\n" +
        "  - nano-banana-2, nano-banana-2-cl, nano-banana-2-2k-cl, nano-banana-2-4k-cl\n" +
        "  - nano-banana-2.1\n" +
        "  - nano-banana-pro, nano-banana-pro-vt, nano-banana-pro-cl\n" +
        "  - nano-banana-pro-vip, nano-banana-pro-4k-vip\n\n" +
        "Grsai API 文档: https://qmy27nhsd9.apifox.cn/")
public class ImageGenRequest {

    @Schema(description = "模型 ID\n\n" +
            "**可选值:**\n" +
            "- dall-e-3, dall-e-2 (OpenAI)\n" +
            "- tt-image-2.5, tt-image-2.6, banana-2.1, gk-image-2.0 (LK888)\n" +
            "- nano-banana, nano-banana-fast, nano-banana-2, nano-banana-2.1, nano-banana-pro 等 (Grsai)",
            example = "nano-banana-2",
            allowableValues = {
                    "dall-e-3", "dall-e-2",
                    "tt-image-2.5", "tt-image-2.6", "banana-2.1", "gk-image-2.0",
                    "nano-banana", "nano-banana-fast", "nano-banana-2", "nano-banana-2-cl",
                    "nano-banana-2-2k-cl", "nano-banana-2-4k-cl", "nano-banana-2.1",
                    "nano-banana-pro", "nano-banana-pro-vt", "nano-banana-pro-cl",
                    "nano-banana-pro-vip", "nano-banana-pro-4k-vip"
            })
    private String model;

    @Schema(description = "图像描述提示词", example = "A cute cat playing with yarn")
    private String prompt;

    @Schema(description = "生成图片数量", example = "1")
    private Integer n = 1;

    @Schema(description = "图片尺寸 (不指定时根据模型使用默认值)", example = "1024x1024")
    private String size;  // 默认为 null，由 Provider 根据模型设置默认值

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
