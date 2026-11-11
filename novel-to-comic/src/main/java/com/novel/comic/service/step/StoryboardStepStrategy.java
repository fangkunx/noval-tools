package com.novel.comic.service.step;

import com.novel.comic.service.AiService;
import com.novel.comic.service.PipelineService;
import com.novel.comic.util.Maps;

import java.util.*;
import java.util.concurrent.*;

/**
 * Step 3: Creates storyboards for each segment, processing in parallel
 * with configurable concurrency.
 */
public class StoryboardStepStrategy implements StepStrategy {

    @Override
    public void execute(StepContext ctx) throws Exception {
        PipelineService p = ctx.pipeline();

        if (p.getSegments().isEmpty())
            throw new RuntimeException("请先执行拆分");

        StringBuilder charRefs = new StringBuilder();
        if (!p.getCharacters().isEmpty()) {
            for (Map<String, String> c : p.getCharacters()) {
                charRefs.append("- **").append(c.get("name")).append("**：")
                        .append(c.get("description"));
                Map<String, String> ta = p.getCharacterTurnarounds().stream()
                        .filter(t -> t.get("name").equals(c.get("name")))
                        .findFirst().orElse(null);
                if (ta != null && ta.containsKey("comboPrompt")
                        && !ta.get("comboPrompt").isEmpty()) {
                    String cp = ta.get("comboPrompt");
                    if (cp.length() > 200) cp = cp.substring(0, 200);
                    charRefs.append("\n  三视图参考: ").append(cp);
                }
                charRefs.append("\n");
            }
        }
        String charRefStr = charRefs.length() > 0 ? charRefs.toString() : "(无已提取角色)";

        List<String> sbList = new ArrayList<>(
                Collections.nCopies(p.getSegments().size(), ""));
        int[] doneCount = {0};
        int total = p.getSegments().size();
        ctx.emit("progress",
                Maps.of("message", "分镜处理 0/" + total, "done", 0, "total", total));

        int concurrency = ctx.config().getConcurrency();
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < p.getSegments().size(); i++) {
            final int segIdx = i;
            final String charRefFinal = charRefStr;
            futures.add(executor.submit(() -> {
                try {
                    String prompt = ctx.ai().loadPrompt("storyboard", Maps.of(
                            "SEGMENT_TEXT", p.getSegments().get(segIdx),
                            "CHARACTER_REFS", charRefFinal
                    ));
                    String text = ctx.ai().callAISmart(
                            "你是专业的Q版漫画分镜导演。严格按照Q版火柴人风格创作分镜。",
                            prompt,
                            new AiService.StreamCallback() {
                                @Override public void onChunk(String text) {
                                    ctx.emit("chunk", Maps.of("text", text, "index", segIdx));
                                    ctx.appendStream(text);
                                }
                                @Override public void onChunk(String text, int index) { onChunk(text); }
                                @Override public void onStatus(String message) {
                                    ctx.emit("status", Maps.of("message", message));
                                }
                                @Override public void onProgress(String message, int done, int total) {}
                            },
                            false, 4096, 0.7
                    );
                    synchronized (sbList) { sbList.set(segIdx, text); }
                } catch (Exception e) {
                    synchronized (sbList) {
                        sbList.set(segIdx, "[失败: " + e.getMessage() + "]");
                    }
                }
                synchronized (doneCount) {
                    doneCount[0]++;
                    ctx.emit("progress",
                            Maps.of("message", "分镜 " + doneCount[0] + "/" + total,
                                    "done", doneCount[0], "total", total));
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(600, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        executor.shutdown();
        p.setStoryboards(sbList);

        long cnt = sbList.stream().filter(s -> s != null && !s.startsWith("[失败")).count();
        ctx.setResult(Maps.of("type", "storyboards", "count", (int) cnt,
                "total", total, "storyboards", sbList));
        ctx.saveOutputFile("storyboard", Maps.of("storyboards", sbList));
    }
}