package com.novel.comic.service.step;

import com.novel.comic.service.PipelineService;
import com.novel.comic.util.Maps;

import java.util.*;

/**
 * Step 5: Assembles final prompts by combining prefix, character refs,
 * storyboard content, and suffix.
 */
public class PromptsStepStrategy implements StepStrategy {

    @Override
    public void execute(StepContext ctx) throws Exception {
        PipelineService p = ctx.pipeline();

        String prefix = ctx.ai().loadPrompt("prefix", null);
        String suffix = ctx.ai().loadPrompt("suffix", null);

        StringBuilder charRefBlock = new StringBuilder();
        if (!p.getCharacterTurnarounds().isEmpty()) {
            charRefBlock.append("\n\n【角色垫图参考】\n");
            for (Map<String, String> ta : p.getCharacterTurnarounds()) {
                String combo = ta.get("comboPrompt");
                if (combo != null && !combo.isEmpty()) {
                    if (combo.length() > 300) combo = combo.substring(0, 300);
                    charRefBlock.append(ta.get("name")).append(": ").append(combo).append("\n");
                }
            }
        }

        List<String> segs = !p.getRenamedSegments().isEmpty()
                ? p.getRenamedSegments() : p.getSegments();
        int total = Math.max(segs.size(), p.getStoryboards().size());
        List<Map<String, Object>> prompts = new ArrayList<>();

        ctx.emit("progress",
                Maps.of("message", "组装 0/" + total, "done", 0, "total", total));

        for (int i = 0; i < total; i++) {
            String sb = i < p.getStoryboards().size() ? p.getStoryboards().get(i) : "";
            String fp = prefix + charRefBlock + "\n\n" + (sb != null ? sb : "") + "\n\n" + suffix;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("index", i + 1);
            entry.put("segment", i < segs.size() ? segs.get(i) : "");
            entry.put("storyboard", sb);
            entry.put("prefix", prefix);
            entry.put("suffix", suffix);
            entry.put("charRefBlock", charRefBlock.toString());
            entry.put("fullPrompt", fp);
            prompts.add(entry);

            ctx.emit("progress",
                    Maps.of("message", "组装 " + (i + 1) + "/" + total,
                            "done", i + 1, "total", total));
        }

        p.setFinalPrompts(prompts);
        ctx.setResult(Maps.of("type", "prompts", "count", prompts.size(), "prompts", prompts));
        ctx.saveOutputFile("prompts", Maps.of("prompts", prompts));
    }
}