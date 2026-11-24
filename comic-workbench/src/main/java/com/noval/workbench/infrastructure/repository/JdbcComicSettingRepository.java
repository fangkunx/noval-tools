package com.noval.workbench.infrastructure.repository;

import com.noval.workbench.domain.model.ComicSetting;
import com.noval.workbench.domain.repository.ComicSettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcComicSettingRepository implements ComicSettingRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcComicSettingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ComicSetting> findByNovelId(Long novelId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM comic_setting WHERE novel_id = ?", novelId);
        if (rows.isEmpty()) return Optional.empty();
        Map<String, Object> row = rows.get(0);
        return Optional.of(new ComicSetting.Builder()
                .id(((Number) row.get("id")).longValue())
                .novelId(((Number) row.get("novel_id")).longValue())
                .textModelId((String) row.get("text_model_id"))
                .imageModelId((String) row.get("image_model_id"))
                .artStyleId((String) row.get("art_style_id"))
                .prefixPrompt((String) row.get("prefix_prompt"))
                .suffixPrompt((String) row.get("suffix_prompt"))
                .aspectRatio((String) row.get("aspect_ratio"))
                .build());
    }

    @Override
    public ComicSetting save(ComicSetting setting) {
        if (setting.getId() == null) {
            jdbcTemplate.update(
                    "INSERT INTO comic_setting (novel_id, text_model_id, image_model_id, art_style_id, prefix_prompt, suffix_prompt, aspect_ratio) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    setting.getNovelId(), setting.getTextModelId(), setting.getImageModelId(),
                    setting.getArtStyleId(), setting.getPrefixPrompt(), setting.getSuffixPrompt(),
                    setting.getAspectRatio());
            Long newId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
            return findByNovelId(setting.getNovelId()).orElse(setting);
        } else {
            jdbcTemplate.update(
                    "UPDATE comic_setting SET text_model_id=?, image_model_id=?, art_style_id=?, prefix_prompt=?, suffix_prompt=?, aspect_ratio=? WHERE id=?",
                    setting.getTextModelId(), setting.getImageModelId(),
                    setting.getArtStyleId(), setting.getPrefixPrompt(), setting.getSuffixPrompt(),
                    setting.getAspectRatio(), setting.getId());
            return findByNovelId(setting.getNovelId()).orElse(setting);
        }
    }
}
