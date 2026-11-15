package com.noval.gateway.interfaces.rest.dto;

import com.noval.gateway.domain.model.ImageComposeCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Image composition request DTO.
 * Combines multiple images into a grid layout.
 */
@Schema(description = "图片合成请求\n\n" +
        "将多张图片合成为一张网格布局图片。\n\n" +
        "**支持的图片数量与布局:**\n" +
        "- 2张 → 1x2 横向布局\n" +
        "- 4张 → 2x2 网格布局\n" +
        "- 6张 → 2x3 网格布局\n" +
        "- 9张 → 3x3 网格布局")
public class ComposeRequest {

    @Schema(description = "模型 ID", example = "tt-image-2.5")
    private String model = "tt-image-2.5";

    @Schema(description = "生成提示词（描述合成效果）",
            example = "Arrange the input images in a strict rectangular grid layout with no gaps. Each image must be a perfect rectangle with sharp 90-degree corners. Add a small white number (001, 002, 003, 004, etc.) in the top-right corner of each image block. Keep the original images unchanged, no blending or artistic effects.")
    private String prompt = "Arrange the input images in a strict rectangular grid layout with no gaps. Each image must be a perfect rectangle with sharp 90-degree corners. Add a small white number (001, 002, 003, 004, etc.) in the top-right corner of each image block. Keep the original images unchanged, no blending or artistic effects.";

    @Schema(description = "输入图片的公网可访问 URL 列表\n\n" +
            "支持数量: 2, 4, 6, 9 张\n\n" +
            "**注意:** 图片必须先上传到七牛云 OSS，获取公网 URL 后再调用此接口",
            example = "[\"https://toj7jniwd.hn-bkt.clouddn.com/compose/1731654321_abc123.jpg\"]",
            required = true)
    private List<String> imageUrls;

    @Schema(description = "图片比例", example = "3:4")
    private String aspectRatio = "3:4";

    @Schema(description = "分辨率", example = "2K")
    private String resolution = "2K";

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

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public ImageComposeCommand toCommand() {
        return ImageComposeCommand.builder()
                .model(model)
                .prompt(prompt)
                .imageUrls(imageUrls)
                .aspectRatio(aspectRatio)
                .resolution(resolution)
                .build();
    }
}
