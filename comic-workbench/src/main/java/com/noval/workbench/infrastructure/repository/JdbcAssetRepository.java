package com.noval.workbench.infrastructure.repository;

import com.noval.workbench.domain.model.Asset;
import com.noval.workbench.domain.model.AssetType;
import com.noval.workbench.domain.model.ContentStyle;
import com.noval.workbench.domain.repository.AssetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcAssetRepository implements AssetRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAssetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Asset> findAll() {
        return jdbcTemplate.query("SELECT * FROM asset", new AssetRowMapper());
    }

    @Override
    public List<Asset> findByType(AssetType type) {
        return jdbcTemplate.query(
                "SELECT * FROM asset WHERE type = ?", new AssetRowMapper(), type.name());
    }

    @Override
    public List<Asset> findByNovelId(Long novelId) {
        return jdbcTemplate.query(
                "SELECT * FROM asset WHERE novel_id = ?", new AssetRowMapper(), novelId);
    }

    @Override
    public Optional<Asset> findById(Long id) {
        List<Asset> results = jdbcTemplate.query(
                "SELECT * FROM asset WHERE id = ?", new AssetRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Asset save(Asset asset) {
        if (asset.getId() == null) {
            Long newId = insert(asset);
            return findById(newId).orElse(asset);
        } else {
            update(asset);
            return findById(asset.getId()).orElse(asset);
        }
    }

    private Long insert(Asset asset) {
        jdbcTemplate.update(
                "INSERT INTO asset (novel_id, name, type, style, description, image_url) VALUES (?, ?, ?, ?, ?, ?)",
                asset.getNovelId(), asset.getName(),
                asset.getType() != null ? asset.getType().name() : null,
                asset.getStyle() != null ? asset.getStyle().name() : null,
                asset.getDescription(), asset.getImageUrl()
        );
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    private void update(Asset asset) {
        jdbcTemplate.update(
                "UPDATE asset SET novel_id=?, name=?, type=?, style=?, description=?, image_url=? WHERE id=?",
                asset.getNovelId(), asset.getName(),
                asset.getType() != null ? asset.getType().name() : null,
                asset.getStyle() != null ? asset.getStyle().name() : null,
                asset.getDescription(), asset.getImageUrl(), asset.getId()
        );
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM asset WHERE id = ?", id);
    }

    private class AssetRowMapper implements RowMapper<Asset> {
        @Override
        public Asset mapRow(ResultSet rs, int rowNum) throws SQLException {
            String typeStr = rs.getString("type");
            String styleStr = rs.getString("style");
            return Asset.builder()
                    .id(rs.getLong("id"))
                    .novelId(rs.getLong("novel_id"))
                    .name(rs.getString("name"))
                    .type(typeStr != null ? AssetType.valueOf(typeStr) : null)
                    .style(styleStr != null ? ContentStyle.valueOf(styleStr) : null)
                    .description(rs.getString("description"))
                    .imageUrl(rs.getString("image_url"))
                    .build();
        }
    }
}
