package com.noval.workbench.infrastructure.repository;

import com.noval.workbench.domain.model.Shot;
import com.noval.workbench.domain.model.ShotState;
import com.noval.workbench.domain.repository.ShotRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JdbcShotRepository implements ShotRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcShotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Shot> findByNovelId(Long novelId) {
        return jdbcTemplate.query(
                "SELECT * FROM shot WHERE novel_id = ? ORDER BY sequence, id",
                new ShotRowMapper(), novelId);
    }

    @Override
    public List<Shot> findByNovelIdPaged(Long novelId, int offset, int limit) {
        // ORDER BY sequence, id：以 id 作为稳定平手键，保证分页结果确定、不重不漏
        return jdbcTemplate.query(
                "SELECT * FROM shot WHERE novel_id = ? ORDER BY sequence, id LIMIT ? OFFSET ?",
                new ShotRowMapper(), novelId, limit, offset);
    }

    @Override
    public int countByNovelId(Long novelId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shot WHERE novel_id = ?",
                Integer.class, novelId);
        return count != null ? count : 0;
    }

    @Override
    public Optional<Shot> findById(Long id) {
        List<Shot> results = jdbcTemplate.query(
                "SELECT * FROM shot WHERE id = ?", new ShotRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Shot save(Shot shot) {
        if (shot.getId() == null) {
            Long newId = insert(shot);
            return findById(newId).orElse(shot);
        } else {
            update(shot);
            return findById(shot.getId()).orElse(shot);
        }
    }

    private Long insert(Shot shot) {
        jdbcTemplate.update(
                "INSERT INTO shot (novel_id, sequence, original_text, scene_description, prompt_text, tags, state, main_image_url, alt_image_urls) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                shot.getNovelId(), shot.getSequence(), shot.getOriginalText(),
                shot.getSceneDescription(), shot.getPromptText(),
                serializeList(shot.getTags()),
                shot.getState().name(),
                shot.getMainImageUrl(), serializeList(shot.getAltImageUrls())
        );
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    private void update(Shot shot) {
        jdbcTemplate.update(
                "UPDATE shot SET novel_id=?, sequence=?, original_text=?, scene_description=?, prompt_text=?, tags=?, state=?, main_image_url=?, alt_image_urls=? WHERE id=?",
                shot.getNovelId(), shot.getSequence(), shot.getOriginalText(),
                shot.getSceneDescription(), shot.getPromptText(),
                serializeList(shot.getTags()),
                shot.getState().name(),
                shot.getMainImageUrl(), serializeList(shot.getAltImageUrls()),
                shot.getId()
        );
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM shot WHERE id = ?", id);
    }

    @Override
    public void deleteByNovelId(Long novelId) {
        jdbcTemplate.update("DELETE FROM shot WHERE novel_id = ?", novelId);
    }

    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join("|", list);
    }

    private List<String> deserializeList(String s) {
        if (s == null || s.isEmpty()) return new ArrayList<>();
        return Arrays.stream(s.split("\\|"))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
    }

    private class ShotRowMapper implements RowMapper<Shot> {
        @Override
        public Shot mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Shot.builder()
                    .id(rs.getLong("id"))
                    .novelId(rs.getLong("novel_id"))
                    .sequence(rs.getInt("sequence"))
                    .originalText(rs.getString("original_text"))
                    .sceneDescription(rs.getString("scene_description"))
                    .promptText(rs.getString("prompt_text"))
                    .tags(deserializeList(rs.getString("tags")))
                    .state(ShotState.valueOf(rs.getString("state")))
                    .mainImageUrl(rs.getString("main_image_url"))
                    .altImageUrls(deserializeList(rs.getString("alt_image_urls")))
                    .build();
        }
    }
}
