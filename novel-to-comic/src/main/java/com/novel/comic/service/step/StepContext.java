package com.novel.comic.service.step;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novel.comic.config.AppConfig;
import com.novel.comic.service.AiService;
import com.novel.comic.service.PipelineService;
import com.novel.comic.service.SseService;
import com.novel.comic.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Execution context passed to each {@link StepStrategy}.
 * Provides the strategy with access to shared pipeline state,
 * services, and convenience methods for SSE emission and file output.
 *
 * <p>This class is in the {@code com.novel.comic.service.step} package
 * so strategies can access {@link PipelineService}'s package-private fields.
 */
public class StepContext {

    private static final Logger log = LoggerFactory.getLogger(StepContext.class);

    private final PipelineService pipeline;
    private final int stepIndex;
    private final AiService aiService;
    private final SseService sseService;
    private final AppConfig.PipelineConfig config;
    private final ObjectMapper mapper;

    public StepContext(PipelineService pipeline, int stepIndex,
                       AiService aiService, SseService sseService,
                       AppConfig.PipelineConfig config, ObjectMapper mapper) {
        this.pipeline = pipeline;
        this.stepIndex = stepIndex;
        this.aiService = aiService;
        this.sseService = sseService;
        this.config = config;
        this.mapper = mapper;
    }

    // ── service accessors ─────────────────────────────────────────

    public int stepIndex() { return stepIndex; }
    public AiService ai() { return aiService; }
    public SseService sse() { return sseService; }
    public AppConfig.PipelineConfig config() { return config; }
    public ObjectMapper mapper() { return mapper; }

    /** Direct access to the pipeline for reading/writing shared state. */
    public PipelineService pipeline() { return pipeline; }

    // ── convenience: SSE ──────────────────────────────────────────

    public void emit(String event, Object data) {
        sseService.broadcast(stepIndex, event, data);
    }

    public void appendStream(String text) {
        sseService.appendStreamText(stepIndex, text);
    }

    // ── convenience: result ───────────────────────────────────────

    public PipelineService.StepState step() {
        return pipeline.getSteps().get(stepIndex);
    }

    public void setResult(Map<String, Object> result) {
        step().result = result;
        emit("result", result);
    }

    // ── convenience: file output ──────────────────────────────────

    public void saveOutputFile(String name, Object data) {
        try {
            Path outputDir = config.getOutputPath();
            if (!Files.exists(outputDir)) Files.createDirectories(outputDir);
            String ts = Instant.now().toString().replace(":", "-").replace(".", "-");
            String title = pipeline.getNovelTitle();
            String fname = (title.isEmpty() ? "output" : title) + "_" + name + "_" + ts + ".json";
            Path fp = outputDir.resolve(fname);
            Files.write(fp, mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(data).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Save failed: {}", e.getMessage());
        }
    }
}