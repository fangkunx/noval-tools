package com.noval.gateway.interfaces.rest;

import com.noval.gateway.application.ImageGenService;
import com.noval.gateway.domain.model.ImageGenCommand;
import com.noval.gateway.domain.model.ImageGenResult;
import com.noval.gateway.interfaces.rest.dto.ErrorResponse;
import com.noval.gateway.interfaces.rest.dto.ImageGenRequest;
import com.noval.gateway.interfaces.rest.dto.ImageGenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
@Tag(name = "Image", description = "图像生成 - OpenAI 兼容协议")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageGenService imageGenService;

    public ImageController(ImageGenService imageGenService) {
        this.imageGenService = imageGenService;
    }

    @Operation(
            summary = "图像生成",
            description = "根据文本提示生成图像，支持 DALL-E 3 等模型"
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
}
