package com.noval.workbench.infrastructure.repository;

import com.noval.workbench.domain.model.Prompt;
import com.noval.workbench.domain.repository.PromptRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPromptRepository implements PromptRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPromptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Prompt> findAll() {
        return jdbcTemplate.query("SELECT * FROM prompt", new PromptRowMapper());
    }

    @Override
    public List<Prompt> findByCategory(String category) {
        return jdbcTemplate.query("SELECT * FROM prompt WHERE category = ?", new PromptRowMapper(), category);
    }

    @Override
    public Optional<Prompt> findById(Long id) {
        List<Prompt> results = jdbcTemplate.query(
                "SELECT * FROM prompt WHERE id = ?", new PromptRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Prompt save(Prompt prompt) {
        if (prompt.getId() == null) {
            Long newId = insert(prompt);
            return findById(newId).orElse(prompt);
        } else {
            update(prompt);
            return findById(prompt.getId()).orElse(prompt);
        }
    }

    private Long insert(Prompt prompt) {
        jdbcTemplate.update(
                "INSERT INTO prompt (title, category, content) VALUES (?, ?, ?)",
                prompt.getTitle(), prompt.getCategory(), prompt.getContent()
        );
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    private void update(Prompt prompt) {
        jdbcTemplate.update(
                "UPDATE prompt SET title=?, category=?, content=? WHERE id=?",
                prompt.getTitle(), prompt.getCategory(), prompt.getContent(), prompt.getId()
        );
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM prompt WHERE id = ?", id);
    }

    private class PromptRowMapper implements RowMapper<Prompt> {
        @Override
        public Prompt mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Prompt.builder()
                    .id(rs.getLong("id"))
                    .title(rs.getString("title"))
                    .category(rs.getString("category"))
                    .content(rs.getString("content"))
                    .build();
        }
    }
}
