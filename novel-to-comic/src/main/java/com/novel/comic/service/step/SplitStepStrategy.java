package com.novel.comic.service.step;

import com.novel.comic.service.AiService;
import com.novel.comic.service.PipelineService;
import com.novel.comic.util.Maps;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step 0: Splits novel text into segments via AI.
 */
public class SplitStepStrategy implements StepStrategy {

    @Override
    public void execute(StepContext ctx) throws Exception {
        PipelineService p = ctx.pipeline();
        int idx = ctx.stepIndex();

        ctx.emit("progress", Maps.of("message", "AI 正在分析小说并拆分...", "done", 0, "total", 1));

        String prompt = ctx.ai().loadPrompt("split", Maps.of(
                "SEGMENT_COUNT", String.valueOf(p.getSegmentCount()),
                "NOVEL_TEXT", p.getNovelText()
        ));

        String raw = ctx.ai().callAISmart(
                "你是专业的漫画分镜导演。只输出拆分结果，每行一段，按编号输出，不要任何多余内容。",
                prompt,
                createStreamCallback(ctx),
                false, 16000, 0.3
        );

        List<String> segs = new ArrayList<>();
        Pattern pattern = Pattern.compile("^\\d+[\\.\\、\\)）]\\s*(.+)");
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            Matcher m = pattern.matcher(line);
            segs.add(m.find() ? m.group(1).trim() : line);
        }
        if (segs.isEmpty()) segs = Arrays.asList(raw.split("\n"));

        p.setSegments(segs);

        ctx.setResult(Maps.of("type", "segments", "count", segs.size(), "segments", segs));
        ctx.saveOutputFile("split", Maps.of("segments", segs));
    }

    // ── shared callback factory ───────────────────────────────────

    static AiService.StreamCallback createStreamCallback(StepContext ctx) {
        return new AiService.StreamCallback() {
            @Override public void onChunk(String text) {
                ctx.emit("chunk", Maps.of("text", text));
                ctx.appendStream(text);
            }
            @Override public void onChunk(String text, int index) { onChunk(text); }
            @Override public void onStatus(String message) {
                ctx.emit("status", Maps.of("message", message));
            }
            @Override public void onProgress(String message, int done, int total) {}
        };
    }
}