package com.noval.workbench.interfaces.rest;

import com.noval.workbench.application.ComicSettingService;
import com.noval.workbench.domain.model.ComicSetting;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 漫画设置控制器
 */
@RestController
@RequestMapping("/api/comic-settings")
public class ComicSettingController {

    private final ComicSettingService comicSettingService;

    public ComicSettingController(ComicSettingService comicSettingService) {
        this.comicSettingService = comicSettingService;
    }

    @GetMapping
    public ResponseEntity<?> getByNovel(@RequestParam Long novelId) {
        return comicSettingService.getByNovelId(novelId)
                .map(s -> ResponseEntity.ok((Object) toMap(s)))
                .orElse(ResponseEntity.ok((Object) new HashMap<>()));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody Map<String, Object> body) {
        Long novelId = ((Number) body.get("novelId")).longValue();

        Optional<ComicSetting> existing = comicSettingService.getByNovelId(novelId);
        ComicSetting setting = new ComicSetting.Builder()
                .id(existing.map(ComicSetting::getId).orElse(null))
                .novelId(novelId)
                .textModelId((String) body.get("textModelId"))
                .imageModelId((String) body.get("imageModelId"))
                .artStyleId((String) body.get("artStyleId"))
                .prefixPrompt((String) body.get("prefixPrompt"))
                .suffixPrompt((String) body.get("suffixPrompt"))
                .aspectRatio((String) body.get("aspectRatio"))
                .build();

        ComicSetting saved = comicSettingService.save(setting);
        return ResponseEntity.ok((Object) toMap(saved));
    }

    private Map<String, Object> toMap(ComicSetting s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("novelId", s.getNovelId());
        map.put("textModelId", s.getTextModelId());
        map.put("imageModelId", s.getImageModelId());
        map.put("artStyleId", s.getArtStyleId());
        map.put("prefixPrompt", s.getPrefixPrompt());
        map.put("suffixPrompt", s.getSuffixPrompt());
        map.put("aspectRatio", s.getAspectRatio());
        return map;
    }
}
