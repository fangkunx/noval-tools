package com.noval.workbench.interfaces.rest;

import com.noval.workbench.application.PresetService;
import com.noval.workbench.domain.model.Preset;
import com.noval.workbench.domain.model.PresetItem;
import com.noval.workbench.domain.model.Novel;
import com.noval.workbench.domain.repository.NovelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 预设控制器
 */
@RestController
@RequestMapping("/api/presets")
public class PresetController {

    private final PresetService presetService;
    private final NovelRepository novelRepository;

    public PresetController(PresetService presetService, NovelRepository novelRepository) {
        this.presetService = presetService;
        this.novelRepository = novelRepository;
    }

    @GetMapping
    public ResponseEntity<?> getByNovel(@RequestParam Long novelId) {
        return presetService.getByNovelId(novelId)
                .map(p -> ResponseEntity.ok((Object) toMap(p)))
                .orElse(ResponseEntity.ok((Object) new HashMap<>()));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody Map<String, Object> body) {
        Long novelId = ((Number) body.get("novelId")).longValue();
        String plotSummary = (String) body.get("plotSummary");
        List<PresetItem> characters = parseItems((List<?>) body.get("characters"));
        List<PresetItem> scenes = parseItems((List<?>) body.get("scenes"));
        List<PresetItem> props = parseItems((List<?>) body.get("props"));

        Optional<Preset> existing = presetService.getByNovelId(novelId);
        Preset preset = new Preset.Builder()
                .id(existing.map(Preset::getId).orElse(null))
                .novelId(novelId)
                .plotSummary(plotSummary)
                .characters(characters)
                .scenes(scenes)
                .props(props)
                .build();

        Preset saved = presetService.save(preset);
        return ResponseEntity.ok((Object) toMap(saved));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam Long novelId) {
        presetService.deleteByNovelId(novelId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 智能生成预设（角色/场景/道具）
     * POST /api/presets/smart?novelId=123&type=character
     * 返回: { "items": [{"name":"...","description":"...","type":"...","imageUrl":""}] }
     */
    @PostMapping("/smart")
    public ResponseEntity<?> smartGenerate(@RequestParam Long novelId, @RequestParam String type) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (!novelOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "小说不存在"));
        }
        Novel novel = novelOpt.get();
        String content = novel.getContent();
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.singletonMap("items", Collections.emptyList()));
        }

        List<PresetItem> items = generateSmartPreset(content, type);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items.stream().map(PresetItem::toJson).collect(Collectors.toList()));
        return ResponseEntity.ok(result);
    }

    /**
     * 基于小说内容智能生成预设条目（当前为规则提取，后续可接入 LLM）
     */
    private List<PresetItem> generateSmartPreset(String content, String type) {
        List<PresetItem> items = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");

        if ("character".equals(type)) {
            // 简单规则：提取含"人物"、"角色"、"他"、"她"的行，或人名模式
            Set<String> seen = new LinkedHashSet<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                // 尝试提取人名（2-4个汉字，后跟动作或对话）
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("([\\u4e00-\\u9fa5]{2,4})(?:[\\s]*[，,。！？：:\\s])").matcher(trimmed);
                while (m.find()) {
                    String name = m.group(1);
                    if (!seen.contains(name) && name.length() >= 2) {
                        seen.add(name);
                        items.add(new PresetItem(name, "", "CHARACTER", null));
                        if (items.size() >= 10) break;
                    }
                }
                if (items.size() >= 10) break;
            }
        } else if ("scene".equals(type)) {
            // 提取含场景关键词的行
            String[] sceneKeywords = {"室内", "室外", "房间", "大厅", "街道", "花园", "森林", "山", "河", "湖", "海", "城", "村", "殿", "堂", "楼", "阁", "寺", "庙"};
            Set<String> seen = new LinkedHashSet<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                for (String kw : sceneKeywords) {
                    if (trimmed.contains(kw) && !seen.contains(trimmed)) {
                        seen.add(trimmed);
                        items.add(new PresetItem(kw + "场景", trimmed, "SCENE", null));
                        if (items.size() >= 10) break;
                    }
                }
                if (items.size() >= 10) break;
            }
        } else if ("prop".equals(type)) {
            // 提取含道具关键词的行
            String[] propKeywords = {"剑", "刀", "枪", "书", "信", "玉", "珠", "宝", "药", "酒", "茶", "花", "灯", "镜", "琴", "棋", "画"};
            Set<String> seen = new LinkedHashSet<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                for (String kw : propKeywords) {
                    if (trimmed.contains(kw) && !seen.contains(kw)) {
                        seen.add(kw);
                        items.add(new PresetItem(kw, trimmed, "PROP", null));
                        if (items.size() >= 10) break;
                    }
                }
                if (items.size() >= 10) break;
            }
        }
        return items;
    }

    private List<PresetItem> parseItems(List<?> raw) {
        if (raw == null) return new ArrayList<>();
        List<PresetItem> items = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Map) {
                items.add(PresetItem.fromJson((Map<String, Object>) o));
            }
        }
        return items;
    }

    private Map<String, Object> toMap(Preset preset) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", preset.getId());
        map.put("novelId", preset.getNovelId());
        map.put("plotSummary", preset.getPlotSummary());
        map.put("characters", preset.getCharacters().stream().map(PresetItem::toJson).collect(Collectors.toList()));
        map.put("scenes", preset.getScenes().stream().map(PresetItem::toJson).collect(Collectors.toList()));
        map.put("props", preset.getProps().stream().map(PresetItem::toJson).collect(Collectors.toList()));
        return map;
    }
}
