package com.noval.workbench.infrastructure.repository;

import com.noval.workbench.domain.model.Account;
import com.noval.workbench.domain.model.AccountCapability;
import com.noval.workbench.domain.repository.AccountRepository;
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
public class JdbcAccountRepository implements AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Account> findAll() {
        return jdbcTemplate.query("SELECT * FROM account", new AccountRowMapper());
    }

    @Override
    public Optional<Account> findById(Long id) {
        List<Account> results = jdbcTemplate.query(
                "SELECT * FROM account WHERE id = ?", new AccountRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Account save(Account account) {
        if (account.getId() == null) {
            Long newId = insert(account);
            return findById(newId).orElse(account);
        } else {
            update(account);
            return findById(account.getId()).orElse(account);
        }
    }

    private Long insert(Account account) {
        jdbcTemplate.update(
                "INSERT INTO account (name, protocol, api_url, api_key, model_id, display_name, capabilities, enabled, vip) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                account.getName(),
                account.getProtocol(),
                account.getApiUrl(),
                account.getApiKey(),
                account.getModelId(),
                account.getDisplayName(),
                serializeCapabilities(account.getCapabilities()),
                account.isEnabled() ? 1 : 0,
                account.isVip() ? 1 : 0
        );
        return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
    }

    private void update(Account account) {
        jdbcTemplate.update(
                "UPDATE account SET name=?, protocol=?, api_url=?, api_key=?, model_id=?, display_name=?, capabilities=?, enabled=?, vip=? WHERE id=?",
                account.getName(),
                account.getProtocol(),
                account.getApiUrl(),
                account.getApiKey(),
                account.getModelId(),
                account.getDisplayName(),
                serializeCapabilities(account.getCapabilities()),
                account.isEnabled() ? 1 : 0,
                account.isVip() ? 1 : 0,
                account.getId()
        );
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM account WHERE id = ?", id);
    }

    private String serializeCapabilities(List<AccountCapability> caps) {
        if (caps == null || caps.isEmpty()) return "";
        return caps.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private List<AccountCapability> deserializeCapabilities(String s) {
        if (s == null || s.isEmpty()) return new ArrayList<>();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(AccountCapability::valueOf)
                .collect(Collectors.toList());
    }

    private class AccountRowMapper implements RowMapper<Account> {
        @Override
        public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Account.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .protocol(rs.getString("protocol"))
                    .apiUrl(rs.getString("api_url"))
                    .apiKey(rs.getString("api_key"))
                    .modelId(rs.getString("model_id"))
                    .displayName(rs.getString("display_name"))
                    .capabilities(deserializeCapabilities(rs.getString("capabilities")))
                    .enabled(rs.getInt("enabled") == 1)
                    .vip(rs.getInt("vip") == 1)
                    .build();
        }
    }
}
