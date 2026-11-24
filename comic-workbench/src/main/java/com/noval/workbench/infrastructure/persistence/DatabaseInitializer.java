package com.noval.workbench.infrastructure.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite 数据库初始化器
 */
@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS account (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "protocol TEXT, " +
                    "api_url TEXT, " +
                    "api_key TEXT, " +
                    "model_id TEXT, " +
                    "display_name TEXT, " +
                    "capabilities TEXT, " +
                    "enabled INTEGER DEFAULT 1, " +
                    "vip INTEGER DEFAULT 0)");

            stmt.execute("CREATE TABLE IF NOT EXISTS prompt (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "category TEXT, " +
                    "content TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS art_style (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "description TEXT, " +
                    "image_url TEXT, " +
                    "usage_count INTEGER DEFAULT 0)");

            stmt.execute("CREATE TABLE IF NOT EXISTS novel (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "content TEXT, " +
                    "source TEXT, " +
                    "vip INTEGER DEFAULT 0, " +
                    "status TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS shot (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "novel_id INTEGER, " +
                    "sequence INTEGER, " +
                    "original_text TEXT, " +
                    "scene_description TEXT, " +
                    "prompt_text TEXT, " +
                    "image_urls TEXT, " +
                    "tags TEXT, " +
                    "state TEXT DEFAULT 'PENDING', " +
                    "main_image_url TEXT, " +
                    "alt_image_urls TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS asset (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "novel_id INTEGER, " +
                    "name TEXT NOT NULL, " +
                    "type TEXT, " +
                    "style TEXT, " +
                    "description TEXT, " +
                    "image_url TEXT)");

            // 兼容旧表：添加新列（如果不存在）
            addColumnIfNotExists(conn, "shot", "main_image_url", "TEXT");
            addColumnIfNotExists(conn, "shot", "alt_image_urls", "TEXT");
        }
    }

    private void addColumnIfNotExists(Connection conn, String table, String column, String type) {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")");
            boolean exists = false;
            while (rs.next()) {
                if (column.equals(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
            rs.close();
            if (!exists) {
                stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
                log.info("已添加列 {}.{}", table, column);
            }
        } catch (SQLException e) {
            log.warn("添加列 {}.{} 失败: {}", table, column, e.getMessage());
        }
    }
}
