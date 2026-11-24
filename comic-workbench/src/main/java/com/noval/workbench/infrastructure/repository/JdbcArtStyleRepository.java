package com.noval.workbench.infrastructure.repository;

import com.noval.workbench.domain.model.ArtStyle;
import com.noval.workbench.domain.repository.ArtStyleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcArtStyleRepository implements ArtStyleRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcArtStyleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ArtStyle> findAll() {
        return jdbcTemplate.query("SELECT * FROM art_style", new ArtStyleRowMapper());
    }

    @Override
    public Optional<ArtStyle> findById(Long id) {
        List<ArtStyle> results = jdbcTemplate.query(
                "SELECT * FROM art_style WHERE id = ?", new ArtStyleRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public ArtStyle save(ArtStyle artStyle) {
        if (artStyle.getId() == null) {
            Long newId = insert(artStyle);
            return findById(newId).orElse(artStyle);
        } else {
            update(artStyle);
            return findById(artStyle.getId()).orElse(artStyle);
        }
    }

    private Long insert(ArtStyle artStyle) {
        jdbcTemplate.update(
                "INSERT INTO art_style (name, description, image_url, usage_count) VALUES (?, ?, ?, ?)",
                artStyle.getName(), artStyle.getDescription(), artStyle.getImageUrl(), artStyle.getUsageCount()
        );
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    private void update(ArtStyle artStyle) {
        jdbcTemplate.update(
                "UPDATE art_style SET name=?, description=?, image_url=?, usage_count=? WHERE id=?",
                artStyle.getName(), artStyle.getDescription(), artStyle.getImageUrl(), artStyle.getUsageCount(), artStyle.getId()
        );
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM art_style WHERE id = ?", id);
    }

    private class ArtStyleRowMapper implements RowMapper<ArtStyle> {
        @Override
        public ArtStyle mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ArtStyle.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .imageUrl(rs.getString("image_url"))
                    .usageCount(rs.getInt("usage_count"))
                    .build();
        }
    }
}
