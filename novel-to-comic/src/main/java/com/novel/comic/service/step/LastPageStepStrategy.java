package com.novel.comic.service.step;

import com.novel.comic.service.PipelineService;
import com.novel.comic.util.Maps;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Step 6: Generates the last-page promotion prompt via AI.
 */
public class LastPageStepStrategy implements StepStrategy {

    @Override
    public void execute(StepContext ctx) throws Exception {
        PipelineService p = ctx.pipeline();

        // Determine primary character name
        String primaryChar = "主角";
        if (!p.getCharacters().isEmpty()) {
            String firstName = p.getCharacters().get(0).get("name");
            for (Map<String, String> mp : p.getNameMapping()) {
                if (firstName != null && firstName.contains(mp.get("from"))) {
                    primaryChar = mp.get("to");
                    break;
                }
            }
            if (primaryChar.equals("主角")) primaryChar = firstName;
        }

        String title = p.getNovelTitle().isEmpty() ? "未命名作品" : p.getNovelTitle();
        String prompt = ctx.ai().loadPrompt("last-page",
                Maps.of("TITLE", title, "CHARACTER_NAME", primaryChar));

        ctx.emit("progress",
                Maps.of("message", "正在生成末张引流...", "done", 0, "total", 1));

        String raw = ctx.ai().callAISmart(
                "你是专业的漫画末页引流策划师。",
                prompt,
                SplitStepStrategy.createStreamCallback(ctx),
                false, 2048, 0.7
        );

        Map<String, Object> lp = new LinkedHashMap<>();
        lp.put("characterName", primaryChar);
        lp.put("title", title);
        lp.put("prompt", raw);
        p.setLastPagePrompt(lp);

        Map<String, Object> result = new LinkedHashMap<>(lp);
        result.put("type", "lastpage");
        ctx.setResult(result);
        ctx.saveOutputFile("lastpage", lp);
    }
}