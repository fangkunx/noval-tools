package com.noval.workbench.interfaces.rest;

import com.noval.workbench.application.PresetService;
import com.noval.workbench.domain.model.Preset;
import com.noval.workbench.domain.model.PresetItem;
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

    public PresetController(PresetService presetService) {
        this.presetService = presetService;
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
