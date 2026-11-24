package com.noval.workbench.infrastructure.repository;

import com.noval.workbench.domain.model.Novel;
import com.noval.workbench.domain.repository.NovelRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcNovelRepository implements NovelRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcNovelRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Novel> findAll() {
        return jdbcTemplate.query("SELECT * FROM novel", new NovelRowMapper());
    }

    @Override
    public Optional<Novel> findById(Long id) {
        List<Novel> results = jdbcTemplate.query(
                "SELECT * FROM novel WHERE id = ?", new NovelRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Novel save(Novel novel) {
        if (novel.getId() == null) {
            Long newId = insert(novel);
            return findById(newId).orElse(novel);
        } else {
            update(novel);
            return findById(novel.getId()).orElse(novel);
        }
    }

    private Long insert(Novel novel) {
        jdbcTemplate.update(
                "INSERT INTO novel (title, content, source, vip, status) VALUES (?, ?, ?, ?, ?)",
                novel.getTitle(), novel.getContent(), novel.getSource(),
                novel.isVip() ? 1 : 0, novel.getStatus()
        );
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    private void update(Novel novel) {
        jdbcTemplate.update(
                "UPDATE novel SET title=?, content=?, source=?, vip=?, status=? WHERE id=?",
                novel.getTitle(), novel.getContent(), novel.getSource(),
                novel.isVip() ? 1 : 0, novel.getStatus(), novel.getId()
        );
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM novel WHERE id = ?", id);
    }

    private class NovelRowMapper implements RowMapper<Novel> {
        @Override
        public Novel mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Novel.builder()
                    .id(rs.getLong("id"))
                    .title(rs.getString("title"))
                    .content(rs.getString("content"))
                    .source(rs.getString("source"))
                    .vip(rs.getInt("vip") == 1)
                    .status(rs.getString("status"))
                    .build();
        }
    }
}
