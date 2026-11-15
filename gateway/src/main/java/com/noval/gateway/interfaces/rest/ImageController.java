package com.noval.gateway.interfaces.rest;

import com.noval.gateway.application.ImageGenService;
import com.noval.gateway.domain.model.ImageEditCommand;
import com.noval.gateway.domain.model.ImageGenCommand;
import com.noval.gateway.domain.model.ImageGenResult;
import com.noval.gateway.interfaces.rest.dto.ErrorResponse;
import com.noval.gateway.interfaces.rest.dto.ImageEditRequest;
import com.noval.gateway.interfaces.rest.dto.ImageGenRequest;
import com.noval.gateway.interfaces.rest.dto.ImageGenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OpenAI-compatible image generation endpoint.
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "Image", description = "图像生成与编辑 - OpenAI 兼容协议\n\n" +
        "**图像生成模型:**\n" +
        "- **OpenAI**: dall-e-3, dall-e-2\n" +
        "- **LK888**: tt-image-2.5, tt-image-2.6, banana-2.1, gk-image-2.0\n" +
        "- **Grsai (Nano Banana 系列)**:\n" +
        "  - nano-banana, nano-banana-fast\n" +
        "  - nano-banana-2, nano-banana-2-cl, nano-banana-2-2k-cl, nano-banana-2-4k-cl\n" +
        "  - nano-banana-2.1\n" +
        "  - nano-banana-pro, nano-banana-pro-vt, nano-banana-pro-cl\n" +
        "  - nano-banana-pro-vip, nano-banana-pro-4k-vip\n" +
        "- **KL**: dall-e-3, dall-e-2, gpt-image-1, flux-pro, flux-dev, flux-schnell\n\n" +
        "**图生图模型 (KL):**\n" +
        "- gpt-image-1, flux-pro, flux-dev, flux-schnell")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageGenService imageGenService;

    public ImageController(ImageGenService imageGenService) {
        this.imageGenService = imageGenService;
    }

    @Operation(
            summary = "图像生成",
            description = "根据文本提示生成图像\n\n" +
                    "**支持的模型:**\n" +
                    "- OpenAI: dall-e-3, dall-e-2\n" +
                    "- LK888: tt-image-2.5, tt-image-2.6, banana-2.1, gk-image-2.0\n" +
                    "- **Grsai Nano Banana 系列**: nano-banana, nano-banana-fast, nano-banana-2, nano-banana-2.1, nano-banana-pro, nano-banana-pro-vip 等\n\n" +
                    "**Grsai API 文档:** https://qmy27nhsd9.apifox.cn/",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "OpenAI DALL-E 3",
                                            summary = "使用 DALL-E 3 生成图像",
                                            value = "{\"model\":\"dall-e-3\",\"prompt\":\"A cute orange cat playing with yarn ball\",\"n\":1,\"size\":\"1024x1024\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Grsai Nano Banana 2",
                                            summary = "使用 Grsai Nano Banana 2 生成图像",
                                            value = "{\"model\":\"nano-banana-2\",\"prompt\":\"一只可爱的橘猫在玩毛线球，数字艺术风格\",\"n\":1,\"size\":\"1024x1024\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Grsai Nano Banana Pro",
                                            summary = "使用 Grsai Nano Banana Pro 生成高质量图像",
                                            value = "{\"model\":\"nano-banana-pro\",\"prompt\":\"A beautiful sunset over the ocean, high quality\",\"n\":1,\"size\":\"1024x1024\"}"
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = ImageGenResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求错误", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授权", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/images/generations")
    public ResponseEntity<?> generateImages(
            @Parameter(description = "图像生成请求", required = true)
            @RequestBody ImageGenRequest request) {
        log.debug("Image gen request: model={}, prompt={}", request.getModel(), request.getPrompt());

        // Convert DTO to domain command
        ImageGenCommand command = ImageGenCommand.builder()
                .model(request.getModel())
                .prompt(request.getPrompt())
                .n(request.getN())
                .size(request.getSize())
                .responseFormat(request.getResponse_format())
                .build();

        ImageGenResult result = imageGenService.generate(command);

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.getError(), "image_gen_error", null));
        }

        ImageGenResponse response = ImageGenResponse.success(
                result.getId(),
                result.getUrls(),
                result.getBase64Data()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "图像编辑 (图生图)",
            description = "根据输入图片和文本提示词编辑图像\n\n" +
                    "**支持的模型:**\n" +
                    "- KL: gpt-image-1, flux-pro, flux-dev, flux-schnell",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "GPT Image 1 图生图",
                                            summary = "使用 GPT Image 1 编辑图像",
                                            value = "{\"model\":\"gpt-image-1\",\"prompt\":\"Add a beautiful sunset background\",\"image\":\"https://example.com/input.png\",\"n\":1,\"size\":\"1024x1024\",\"strength\":0.75}"
                                    ),
                                    @ExampleObject(
                                            name = "Flux Pro 图生图",
                                            summary = "使用 Flux Pro 编辑图像",
                                            value = "{\"model\":\"flux-pro\",\"prompt\":\"Transform to watercolor style\",\"image\":\"data:image/png;base64,iVBOR...\",\"n\":1,\"size\":\"1024x1024\"}"
                                    ),
                                    @ExampleObject(
                                            name = "局部重绘 (Inpainting)",
                                            summary = "使用遮罩进行局部重绘",
                                            value = "{\"model\":\"gpt-image-1\",\"prompt\":\"Replace the sky with stars\",\"image\":\"https://example.com/input.png\",\"mask\":\"https://example.com/mask.png\",\"n\":1,\"size\":\"1024x1024\"}"
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = ImageGenResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求错误", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授权", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/images/edits")
    public ResponseEntity<?> editImages(
            @Parameter(description = "图像编辑请求", required = true)
            @RequestBody ImageEditRequest request) {
        log.debug("Image edit request: model={}, prompt={}, image={}",
                request.getModel(), request.getPrompt(),
                request.getImage() != null ? request.getImage().substring(0, Math.min(50, request.getImage().length())) + "..." : "null");

        // Convert DTO to domain command
        ImageEditCommand command = ImageEditCommand.builder()
                .model(request.getModel())
                .prompt(request.getPrompt())
                .image(request.getImage())
                .mask(request.getMask())
                .n(request.getN())
                .size(request.getSize())
                .responseFormat(request.getResponse_format())
                .strength(request.getStrength())
                .build();

        ImageGenResult result = imageGenService.edit(command);

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.getError(), "image_edit_error", null));
        }

        ImageGenResponse response = ImageGenResponse.success(
                result.getId(),
                result.getUrls(),
                result.getBase64Data()
        );

        return ResponseEntity.ok(response);
    }
}
