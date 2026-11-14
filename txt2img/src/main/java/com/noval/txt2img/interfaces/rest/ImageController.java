package com.noval.txt2img.interfaces.rest;

import com.noval.txt2img.application.ImageGenService;
import com.noval.txt2img.domain.model.BackendType;
import com.noval.txt2img.domain.model.ImageGenResult;
import com.noval.txt2img.domain.model.ModelSpec;
import com.noval.txt2img.interfaces.rest.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for image generation.
 * Thin layer: maps HTTP requests to application service, DTOs to domain objects.
 */
@Controller
public class ImageController {

    private final ImageGenService imageGenService;

    @Autowired
    public ImageController(ImageGenService imageGenService) {
        this.imageGenService = imageGenService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Submit an image generation request.
     */
    @PostMapping("/api/generate")
    @ResponseBody
    public ResponseEntity<?> generate(@RequestBody GenerateRequest req) {
        try {
            BackendType backend = BackendType.from(req.getBackend());
            ImageGenResult result = imageGenService.generate(req.toCommand(), backend);
            return ResponseEntity.ok(TaskStatusResponse.from(result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("error", e.getMessage()));
        }
    }

    /**
     * Poll async task status (lk888 only).
     */
    @GetMapping("/api/status/{taskId}")
    @ResponseBody
    public ResponseEntity<?> pollStatus(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "false") boolean wait) {
        try {
            ImageGenResult result = imageGenService.poll(taskId, BackendType.LK888, wait);
            return ResponseEntity.ok(TaskStatusResponse.from(result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("error", e.getMessage()));
        }
    }

    /**
     * Return available models and their parameter specs.
     */
    @GetMapping("/api/models")
    @ResponseBody
    public ResponseEntity<?> models(@RequestParam(defaultValue = "image") String type) {
        List<ModelSpec> specs = imageGenService.getSupportedModels(null);
        List<ModelSpecResponse> dtos = new ArrayList<>();
        for (ModelSpec s : specs) {
            dtos.add(ModelSpecResponse.from(s));
        }

        // Envelope format for frontend compatibility: { type, models }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("models", dtos);
        return ResponseEntity.ok(data);
    }
}