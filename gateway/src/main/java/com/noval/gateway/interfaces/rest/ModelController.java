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
@Tag(name = "Models", description = "模型列表 - 查询所有可用模型")
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
