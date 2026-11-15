package com.noval.gateway.interfaces.rest;

import com.noval.gateway.application.ModelService;
import com.noval.gateway.domain.model.Capability;
import com.noval.gateway.domain.model.ModelSpec;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAI-compatible models endpoint.
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "Models", description = "模型列表 - 查询所有可用模型\n\n" +
        "**当前支持的 Provider 和模型:**\n" +
        "- **OpenAI**: gpt-4o, gpt-4o-mini, gpt-4-turbo, dall-e-3, dall-e-2\n" +
        "- **Claude**: claude-opus-5-5, claude-sonnet-5-5, claude-haiku-5-5\n" +
        "- **Google Gemini**: gemini-2.0-flash, gemini-1.5-pro, gemini-1.5-flash\n" +
        "- **DeepSeek**: deepseek-chat\n" +
        "- **LK888**: tt-image-2.5, tt-image-2.6, banana-2.1, gk-image-2.0\n" +
        "- **Grsai**: nano-banana 系列图像生成模型，以及通过代理的 chat 模型\n\n" +
        "使用 `?capability=IMAGE_GEN` 可按能力过滤模型。")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @Operation(
            summary = "列出所有模型",
            description = "返回所有可用模型，可按能力过滤（CHAT/VISION/IMAGE_GEN）"
    )
    @ApiResponse(responseCode = "200", description = "成功")
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> listModels(
            @Parameter(description = "按能力过滤：CHAT, VISION, IMAGE_GEN")
            @RequestParam(required = false) String capability) {

        List<ModelSpec> models;
        if (capability != null && !capability.isEmpty()) {
            models = modelService.getModelsByCapability(Capability.from(capability));
        } else {
            models = modelService.getAllModels();
        }

        List<Map<String, Object>> modelList = models.stream()
                .map(this::modelToMap)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("object", "list");
        response.put("data", modelList);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "获取单个模型信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "404", description = "模型不存在")
    })
    @GetMapping("/models/{modelId}")
    public ResponseEntity<?> getModel(
            @Parameter(description = "模型 ID", required = true)
            @PathVariable String modelId) {
        List<ModelSpec> all = modelService.getAllModels();
        for (ModelSpec model : all) {
            if (model.getId().equals(modelId)) {
                return ResponseEntity.ok(modelToMap(model));
            }
        }
        return ResponseEntity.notFound().build();
    }

    private Map<String, Object> modelToMap(ModelSpec model) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", model.getId());
        map.put("object", "model");
        map.put("created", System.currentTimeMillis() / 1000);
        map.put("owned_by", model.getProviderType().name().toLowerCase());
        map.put("name", model.getName());

        List<String> caps = model.getCapabilities().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        map.put("capabilities", caps);

        return map;
    }
}
