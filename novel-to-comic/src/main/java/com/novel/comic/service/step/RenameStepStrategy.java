package com.novel.comic.service.step;

import com.novel.comic.service.PipelineService;
import com.novel.comic.util.Maps;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step 4: Renames characters and synchronizes names throughout the text via AI.
 */
public class RenameStepStrategy implements StepStrategy {

    @Override
    public void execute(StepContext ctx) throws Exception {
        PipelineService p = ctx.pipeline();

        String text = !p.getSegments().isEmpty()
                ? String.join("\n\n", p.getSegments())
                : p.getNovelText();

        String prompt = ctx.ai().loadPrompt("character-rename",
                Maps.of("NOVEL_TEXT", text));

        ctx.emit("progress",
                Maps.of("message", "AI 正在替换角色名字...", "done", 0, "total", 1));

        String raw = ctx.ai().callAISmart(
                "你是专业的文字编辑。替换角色名字，保留姓氏，修改名字。列出对照表。",
                prompt,
                SplitStepStrategy.createStreamCallback(ctx),
                false, 16000, 0.3
        );

        // Parse name mapping
        List<Map<String, String>> mapping = new ArrayList<>();
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.contains("|")) {
                String[] cells = line.split("\\|");
                if (cells.length >= 2) {
                    mapping.add(Maps.of("from", cells[0].trim(), "to", cells[1].trim()));
                }
            }
        }
        if (mapping.isEmpty()) {
            Pattern arrow = Pattern.compile("(.+?)\\s*[→>]\\s*(.+)");
            Matcher m = arrow.matcher(raw);
            while (m.find()) {
                mapping.add(Maps.of("from", m.group(1).trim(), "to", m.group(2).trim()));
            }
        }

        p.setNameMapping(mapping);
        p.setRenamedText(raw);

        // Apply renames to segments
        if (!mapping.isEmpty() && !p.getSegments().isEmpty()) {
            List<String> rs = new ArrayList<>();
            for (String seg : p.getSegments()) {
                String r = seg;
                for (Map<String, String> mp : mapping) {
                    String from = mp.get("from");
                    String to = mp.get("to");
                    if (from != null && to != null && !from.equals(to)) {
                        r = r.replaceAll(Pattern.quote(from), to);
                    }
                }
                rs.add(r);
            }
            p.setRenamedSegments(rs);
        } else {
            p.setRenamedSegments(new ArrayList<>(p.getSegments()));
        }

        ctx.setResult(Maps.of("type", "rename", "mapping", mapping,
                "renamedSegments", p.getRenamedSegments(),
                "preview", raw.length() > 2000 ? raw.substring(0, 2000) : raw));
        ctx.saveOutputFile("rename",
                Maps.of("mapping", mapping, "renamedSegments", p.getRenamedSegments()));
    }
}