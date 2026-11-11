package com.novel.comic.service.step;

import com.novel.comic.service.PipelineService;
import com.novel.comic.util.Maps;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step 2: Generates character turnaround view prompts via AI.
 */
public class TurnaroundStepStrategy implements StepStrategy {

    @Override
    public void execute(StepContext ctx) throws Exception {
        PipelineService p = ctx.pipeline();

        if (p.getCharacters().isEmpty())
            throw new RuntimeException("请先执行角色提取");

        StringBuilder charList = new StringBuilder();
        for (Map<String, String> c : p.getCharacters()) {
            charList.append("**").append(c.get("name")).append("**：")
                    .append(c.get("description")).append("\n");
        }

        ctx.emit("progress", Maps.of("message", "AI 正在生成角色三视图...", "done", 0, "total", 1));

        String prompt = ctx.ai().loadPrompt("character-turnaround",
                Maps.of("CHARACTER_LIST", charList.toString()));

        String raw = ctx.ai().callAISmart(
                "你是Q版火柴人漫画角色设计师。为每个角色生成三视图prompt，用===分隔。",
                prompt,
                SplitStepStrategy.createStreamCallback(ctx),
                false, 4096, 0.7
        );

        List<Map<String, String>> turnarounds = new ArrayList<>();
        String[] sections = raw.split("===+\\s*");
        for (String section : sections) {
            section = section.trim();
            if (section.isEmpty()) continue;
            String[] lines = section.split("\n");
            String name = lines[0].trim().replaceAll("[：:]", "").trim();
            String body = String.join("\n",
                    Arrays.copyOfRange(lines, 1, lines.length)).trim();
            if (name.isEmpty() || body.isEmpty()) continue;

            Map<String, String> ta = new LinkedHashMap<>();
            ta.put("name", name);
            ta.put("frontPrompt", extractView(body, "【正面视图[^】]*】"));
            ta.put("backPrompt", extractView(body, "【背面视图[^】]*】"));
            ta.put("sidePrompt", extractView(body, "【侧面视图[^】]*】"));
            String combo = extractView(body, "【综合三视图[^】]*】");
            ta.put("comboPrompt", combo.isEmpty() ? body : combo);
            ta.put("raw", body);
            turnarounds.add(ta);
        }
        p.setCharacterTurnarounds(turnarounds);

        List<String> charNames = new ArrayList<>();
        for (Map<String, String> c : p.getCharacters()) charNames.add(c.get("name"));

        ctx.setResult(Maps.of("type", "turnarounds", "count", turnarounds.size(),
                "characters", charNames, "turnarounds", turnarounds));
        ctx.saveOutputFile("turnaround",
                Maps.of("turnarounds", turnarounds, "characters", p.getCharacters()));
    }

    private String extractView(String body, String labelPattern) {
        Pattern p = Pattern.compile(labelPattern + "\\s*([\\s\\S]*?)(?=【|---|$)");
        Matcher m = p.matcher(body);
        return m.find() ? m.group(1).trim() : "";
    }
}