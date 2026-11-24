package com.noval.workbench.infrastructure.persistence;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite 数据库初始化器
 */
@Component
public class DatabaseInitializer {

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
                    "state TEXT DEFAULT 'PENDING')");

            stmt.execute("CREATE TABLE IF NOT EXISTS asset (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "novel_id INTEGER, " +
                    "name TEXT NOT NULL, " +
                    "type TEXT, " +
                    "style TEXT, " +
                    "description TEXT, " +
                    "image_url TEXT)");
        }
    }
}
