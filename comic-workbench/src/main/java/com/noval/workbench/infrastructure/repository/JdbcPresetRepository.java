package com.noval.workbench.infrastructure.repository;

import com.noval.workbench.domain.model.Preset;
import com.noval.workbench.domain.model.PresetItem;
import com.noval.workbench.domain.repository.PresetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class JdbcPresetRepository implements PresetRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public JdbcPresetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Preset> findByNovelId(Long novelId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM preset WHERE novel_id = ?", novelId);
        if (rows.isEmpty()) return Optional.empty();
        Map<String, Object> row = rows.get(0);
        Preset preset = new Preset.Builder()
                .id(((Number) row.get("id")).longValue())
                .novelId(((Number) row.get("novel_id")).longValue())
                .plotSummary((String) row.get("plot_summary"))
                .characters(parseItems((String) row.get("characters")))
                .scenes(parseItems((String) row.get("scenes")))
                .props(parseItems((String) row.get("props")))
                .build();
        return Optional.of(preset);
    }

    @Override
    public Preset save(Preset preset) {
        if (preset.getId() == null) {
            jdbcTemplate.update(
                    "INSERT INTO preset (novel_id, plot_summary, characters, scenes, props) VALUES (?, ?, ?, ?, ?)",
                    preset.getNovelId(), preset.getPlotSummary(),
                    serializeItems(preset.getCharacters()),
                    serializeItems(preset.getScenes()),
                    serializeItems(preset.getProps()));
            Long newId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
            return findByNovelId(preset.getNovelId()).orElse(preset);
        } else {
            jdbcTemplate.update(
                    "UPDATE preset SET plot_summary=?, characters=?, scenes=?, props=? WHERE id=?",
                    preset.getPlotSummary(),
                    serializeItems(preset.getCharacters()),
                    serializeItems(preset.getScenes()),
                    serializeItems(preset.getProps()),
                    preset.getId());
            return findByNovelId(preset.getNovelId()).orElse(preset);
        }
    }

    @Override
    public void deleteByNovelId(Long novelId) {
        jdbcTemplate.update("DELETE FROM preset WHERE novel_id = ?", novelId);
    }

    private List<PresetItem> parseItems(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            List<?> list = mapper.readValue(json, List.class);
            List<PresetItem> items = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map) {
                    items.add(PresetItem.fromJson((Map<String, Object>) o));
                }
            }
            return items;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String serializeItems(List<PresetItem> items) {
        if (items == null || items.isEmpty()) return "[]";
        try {
            List<Map<String, Object>> maps = new ArrayList<>();
            for (PresetItem item : items) {
                maps.add(item.toJson());
            }
            return mapper.writeValueAsString(maps);
        } catch (Exception e) {
            return "[]";
        }
    }
}
