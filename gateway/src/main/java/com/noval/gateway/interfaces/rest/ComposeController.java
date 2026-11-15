package com.noval.gateway.interfaces.rest;

import com.noval.gateway.application.ImageComposeService;
import com.noval.gateway.domain.model.ImageComposeCommand;
import com.noval.gateway.domain.model.ImageGenResult;
import com.noval.gateway.infrastructure.config.ComposeConfig;
import com.noval.gateway.interfaces.rest.dto.ComposeRequest;
import com.noval.gateway.interfaces.rest.dto.ComposeResponse;
import com.noval.gateway.interfaces.rest.dto.ErrorResponse;
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

import java.util.Map;

/**
 * Image composition endpoint - combines multiple images into a grid layout.
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "Image Compose", description = "图片合成 - 多图网格合成\n\n" +
        "将多张图片合成为一张网格布局图片。\n\n" +
        "**支持的布局:**\n" +
        "- 2张 → 1x2 横向布局\n" +
        "- 4张 → 2x2 网格布局\n" +
        "- 6张 → 2x3 网格布局\n" +
        "- 9张 → 3x3 网格布局\n\n" +
        "**支持的模型:** LK888 tt-image-2.5")
public class ComposeController {

    private static final Logger log = LoggerFactory.getLogger(ComposeController.class);

    private final ImageComposeService composeService;
    private final ComposeConfig composeConfig;

    public ComposeController(ImageComposeService composeService, ComposeConfig composeConfig) {
        this.composeService = composeService;
        this.composeConfig = composeConfig;
    }

    @Operation(
            summary = "图片合成",
            description = "将多张图片合成为网格布局图片\n\n" +
                    "**支持的图片数量与布局:**\n" +
                    "- 2张 → 1x2 横向布局\n" +
                    "- 4张 → 2x2 网格布局\n" +
                    "- 6张 → 2x3 网格布局\n" +
                    "- 9张 → 3x3 网格布局",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "2x2 网格合成",
                                            summary = "将 4 张图片合成为 2x2 网格",
                                            value = "{\n" +
                                                    "  \"model\": \"tt-image-2.5\",\n" +
                                                    "  \"prompt\": \"combine these 4 images into a 2x2 grid layout, seamless, high quality\",\n" +
                                                    "  \"images\": [\n" +
                                                    "    \"data:image/png;base64,iVBOR...\",\n" +
                                                    "    \"data:image/png;base64,iVBOR...\",\n" +
                                                    "    \"data:image/png;base64,iVBOR...\",\n" +
                                                    "    \"data:image/png;base64,iVBOR...\"\n" +
                                                    "  ],\n" +
                                                    "  \"aspectRatio\": \"3:4\",\n" +
                                                    "  \"resolution\": \"2K\"\n" +
                                                    "}"
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = ComposeResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求错误", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授权", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/images/compose")
    public ResponseEntity<?> composeImages(
            @Parameter(description = "图片合成请求", required = true)
            @RequestBody ComposeRequest request) {
        log.debug("Image compose request: model={}, imageCount={}",
                request.getModel(),
                request.getImageUrls() != null ? request.getImageUrls().size() : 0);

        // Convert DTO to domain command
        ImageComposeCommand command = request.toCommand();

        ImageGenResult result = composeService.compose(command);

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(ComposeResponse.error(result.getError()));
        }

        ComposeResponse response = ComposeResponse.success(result);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "获取提示词模板",
            description = "获取所有预配置的提示词模板，用于图片合成"
    )
    @GetMapping("/images/compose/templates")
    public ResponseEntity<Map<String, String>> getPromptTemplates() {
        return ResponseEntity.ok(composeConfig.getPromptTemplates());
    }
}
