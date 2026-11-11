package com.novel.comic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novel.comic.config.AppConfig;
import com.novel.comic.service.PipelineService;
import com.novel.comic.service.SseService;
import com.novel.comic.service.TaskStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PipelineController {
    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    private final PipelineService pipelineService;
    private final SseService sseService;
    private final TaskStore taskStore;
    private final AppConfig.PipelineConfig config;
    private final ObjectMapper mapper;

    public PipelineController(PipelineService pipelineService, SseService sseService,
                              TaskStore taskStore,
                              AppConfig.PipelineConfig config, ObjectMapper mapper) {
        this.pipelineService = pipelineService;
        this.sseService = sseService;
        this.taskStore = taskStore;
        this.config = config;
        this.mapper = mapper;
    }

    // ─── Status ──────────────────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(pipelineService.buildStatusResponse());
    }

    // ─── Init ────────────────────────────────────────────────────────

    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> init(@RequestBody Map<String, Object> body) {
        String text = (String) body.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(com.novel.comic.util.Maps.of("error", "小说文本不能为空"));
        }
        int segCount = body.get("segmentCount") instanceof Number
                ? ((Number) body.get("segmentCount")).intValue() : config.getDefaultSegments();
        String title = (String) body.getOrDefault("title", "未命名作品");

        return ResponseEntity.ok(pipelineService.init(text, segCount, title));
    }

    // ─── Upload ──────────────────────────────────────────────────────

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(com.novel.comic.util.Maps.of("error", "请上传文件"));
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".txt")) {
            return ResponseEntity.badRequest().body(com.novel.comic.util.Maps.of("error", "仅支持 .txt 文件"));
        }

        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            String decodedName = safeDecode(originalName);
            String title = safeDecode(
                    originalName.contains(".")
                            ? originalName.substring(0, originalName.lastIndexOf('.'))
                            : originalName
            );

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("text", text);
            resp.put("title", title);
            resp.put("size", text.length());
            resp.put("filename", decodedName);
            return ResponseEntity.ok(resp);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(com.novel.comic.util.Maps.of("error", "读取失败: " + e.getMessage()));
        }
    }

    /**
     * Decode filename (multer/busboy may encode UTF-8 as latin1 on Windows).
     * In Java, this is typically not needed since Spring handles encoding correctly,
     * but we keep it for API compatibility.
     */
    private String safeDecode(String name) {
        if (name == null) return null;
        // Already valid CJK
        if (name.matches(".*[一-鿿].*")) return name;
        // Try decoding from latin1
        try {
            byte[] bytes = name.getBytes(StandardCharsets.ISO_8859_1);
            String decoded = new String(bytes, StandardCharsets.UTF_8);
            if (decoded.matches(".*[一-鿿].*")) return decoded;
        } catch (Exception ignored) {}
        return name;
    }

    // ─── Start Step ─────────────────────────────────────────────────

    @PostMapping("/step/{stepIndex}")
    public ResponseEntity<Map<String, Object>> startStep(@PathVariable int stepIndex) {
        if (stepIndex < 0 || stepIndex >= PipelineService.STEP_DEFS.size()) {
            return ResponseEntity.badRequest().body(com.novel.comic.util.Maps.of("error", "无效步骤"));
        }
        if (pipelineService.isRunning(stepIndex)) {
            return ResponseEntity.ok(com.novel.comic.util.Maps.of("success", true, "status", "already_running"));
        }
        pipelineService.executeStep(stepIndex);
        return ResponseEntity.ok(com.novel.comic.util.Maps.of("success", true, "status", "started"));
    }

    // ─── SSE Stream ──────────────────────────────────────────────────

    @GetMapping("/step/{stepIndex}/stream")
    public SseEmitter streamStep(@PathVariable int stepIndex) {
        if (stepIndex < 0 || stepIndex >= PipelineService.STEP_DEFS.size()) {
            SseEmitter err = new SseEmitter();
            err.completeWithError(new IllegalArgumentException("无效步骤"));
            return err;
        }

        PipelineService.StepState step = pipelineService.getStep(stepIndex);
        SseEmitter emitter = sseService.createEmitter(stepIndex);

        // If step is already done, send result immediately
        if ("done".equals(step.status)) {
            try {
                emitter.send(SseEmitter.event().name("status")
                        .data(com.novel.comic.util.Maps.of("status", "done", "step", stepIndex, "name", step.name)));
                String streamText = sseService.getStreamText(stepIndex);
                if (streamText != null && !streamText.isEmpty()) {
                    emitter.send(SseEmitter.event().name("chunk")
                            .data(com.novel.comic.util.Maps.of("text", streamText, "replay", true)));
                }
                if (step.result != null) {
                    emitter.send(SseEmitter.event().name("result").data(step.result));
                }
                emitter.send(SseEmitter.event().name("done").data(new HashMap<>()));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // If step is in error state
        if ("error".equals(step.status)) {
            try {
                emitter.send(SseEmitter.event().name("status")
                        .data(com.novel.comic.util.Maps.of("status", "error", "step", stepIndex)));
                emitter.send(SseEmitter.event().name("error")
                        .data(com.novel.comic.util.Maps.of("message", step.error != null ? step.error : "未知错误")));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Step is running — replay existing stream text first
        String existing = sseService.getStreamText(stepIndex);
        if (existing != null && !existing.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("chunk")
                        .data(com.novel.comic.util.Maps.of("text", existing, "replay", true)));
            } catch (IOException e) {
                emitter.completeWithError(e);
                return emitter;
            }
        }

        try {
            emitter.send(SseEmitter.event().name("status")
                    .data(com.novel.comic.util.Maps.of("status", step.status, "step", stepIndex, "name", step.name)));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        // Events are pushed via SseService.broadcast() from PipelineService
        // The emitter was already registered in createEmitter()
        return emitter;
    }

    // ─── Results ────────────────────────────────────────────────────

    @GetMapping("/results/{stepId}")
    public ResponseEntity<?> getResult(@PathVariable String stepId) {
        PipelineService.StepState step = pipelineService.getStepById(stepId);
        if (step == null || step.result == null) {
            return ResponseEntity.status(404).body(com.novel.comic.util.Maps.of("error", "该步骤尚未执行"));
        }
        return ResponseEntity.ok(step.result);
    }

    @GetMapping("/results")
    public ResponseEntity<Map<String, Object>> getAllResults() {
        Map<String, Object> results = new LinkedHashMap<>();
        for (PipelineService.StepState s : pipelineService.getSteps()) {
            if (s.result != null) {
                results.put(s.id, s.result);
            }
        }
        return ResponseEntity.ok(results);
    }

    // ─── Download ───────────────────────────────────────────────────

    @GetMapping("/download")
    public ResponseEntity<byte[]> download() {
        Map<String, Object> data = pipelineService.buildDownloadData();
        try {
            byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
            String filename = URLEncoder.encode(
                    (pipelineService.getNovelTitle().isEmpty() ? "output" : pipelineService.getNovelTitle())
                            + "_output.json",
                    "UTF-8").replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                    .body(json);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── State ──────────────────────────────────────────────────────

    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> checkState() {
        return ResponseEntity.ok(pipelineService.buildStateResponse());
    }

    // ─── Rerun ──────────────────────────────────────────────────────

    @PostMapping("/rerun/{stepIndex}")
    public ResponseEntity<Map<String, Object>> rerun(@PathVariable int stepIndex) {
        if (stepIndex < 0 || stepIndex >= PipelineService.STEP_DEFS.size()) {
            return ResponseEntity.badRequest().body(com.novel.comic.util.Maps.of("error", "无效步骤"));
        }
        if (pipelineService.isRunning(stepIndex)) {
            return ResponseEntity.status(409).body(com.novel.comic.util.Maps.of("error", "该步骤正在运行中"));
        }
        return ResponseEntity.ok(pipelineService.rerunFrom(stepIndex));
    }

    // ─── Projects ────────────────────────────────────────────────────

    @GetMapping("/projects")
    public ResponseEntity<List<Map<String, Object>>> listProjects() {
        return ResponseEntity.ok(taskStore.listProjects());
    }

    @PostMapping("/projects/{taskId}/load")
    public ResponseEntity<Map<String, Object>> loadProject(@PathVariable String taskId) {
        boolean ok = pipelineService.loadTask(taskId);
        if (!ok) {
            return ResponseEntity.status(404)
                    .body(com.novel.comic.util.Maps.of("error", "项目不存在"));
        }
        return ResponseEntity.ok(pipelineService.buildStateResponse());
    }

    @DeleteMapping("/projects/{taskId}")
    public ResponseEntity<Map<String, String>> deleteProject(@PathVariable String taskId) {
        boolean ok = taskStore.deleteTask(taskId);
        if (!ok) {
            return ResponseEntity.status(404)
                    .body(com.novel.comic.util.Maps.of("error", "项目不存在"));
        }
        // If we deleted the currently loaded project, reset
        if (taskId.equals(pipelineService.getCurrentTaskId())) {
            pipelineService.resetPipeline();
            sseService.clearAll();
        }
        return ResponseEntity.ok(com.novel.comic.util.Maps.of("success", "true"));
    }

    // ─── Reset ──────────────────────────────────────────────────────

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        pipelineService.clearStateFile();
        sseService.clearAll();
        return ResponseEntity.ok(com.novel.comic.util.Maps.of("success", "true"));
    }
}