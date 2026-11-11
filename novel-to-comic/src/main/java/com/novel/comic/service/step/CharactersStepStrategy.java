package com.novel.comic.service.step;

import com.novel.comic.service.PipelineService;
import com.novel.comic.util.Maps;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step 1: Extracts characters from the novel and generates descriptions via AI.
 */
public class CharactersStepStrategy implements StepStrategy {

    @Override
    public void execute(StepContext ctx) throws Exception {
        PipelineService p = ctx.pipeline();

        ctx.emit("progress", Maps.of("message", "AI 正在提取角色...", "done", 0, "total", 1));

        String prompt = ctx.ai().loadPrompt("character-extract",
                Maps.of("NOVEL_TEXT", p.getNovelText()));

        String raw = ctx.ai().callAISmart(
                "你是经验丰富的漫画人物设计大师。只输出角色信息，格式：角色名：描述。",
                prompt,
                SplitStepStrategy.createStreamCallback(ctx),
                false, 4096, 0.3
        );

        List<Map<String, String>> chars = new ArrayList<>();
        Pattern ptn = Pattern.compile("^(.+?)[：:]\\s*(.+)");
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            Matcher m = ptn.matcher(line);
            if (m.find()) {
                chars.add(Maps.of("name", m.group(1).trim(), "description", m.group(2).trim()));
            }
        }
        p.setCharacters(chars);

        ctx.setResult(Maps.of("type", "characters", "count", chars.size(), "characters", chars));
        ctx.saveOutputFile("characters", Maps.of("characters", chars));
    }
}