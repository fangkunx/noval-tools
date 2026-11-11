package com.novel.comic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SQLite persistence for novel-to-comic pipeline.
 *
 * <p>Each step has its own dedicated table for its output data,
 * plus a {@code step_status} table for execution tracking.
 */
@Service
public class TaskStore {

    private static final Logger log = LoggerFactory.getLogger(TaskStore.class);

    private final String dbUrl;
    private final ObjectMapper mapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public TaskStore(ObjectMapper mapper) {
        this.mapper = mapper;
        Path dbPath = Paths.get("").toAbsolutePath().resolve("pipeline.db");
        this.dbUrl = "jdbc:sqlite:" + dbPath.toString().replace("\\", "/");
        initSchema();
        log.info("SQLite database at {}", dbPath);
    }

    // ── Schema ───────────────────────────────────────────────────────

    private void initSchema() {
        try (Connection c = connect()) {
            Statement stmt = c.createStatement();
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");

            // ── task metadata ──
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS novel_tasks (" +
                "  id              TEXT PRIMARY KEY," +
                "  title           TEXT NOT NULL," +
                "  novel_text      TEXT NOT NULL DEFAULT ''," +
                "  segment_count   INTEGER NOT NULL DEFAULT 90," +
                "  current_step    INTEGER NOT NULL DEFAULT -1," +
                "  status          TEXT NOT NULL DEFAULT 'CREATED'," +
                "  created_at      TEXT NOT NULL," +
                "  updated_at      TEXT NOT NULL" +
                ")"
            );

            // ── step execution tracking ──
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS step_status (" +
                "  task_id     TEXT NOT NULL REFERENCES novel_tasks(id) ON DELETE CASCADE," +
                "  step_index  INTEGER NOT NULL," +
                "  step_id     TEXT NOT NULL," +
                "  step_name   TEXT NOT NULL," +
                "  step_icon   TEXT NOT NULL DEFAULT ''," +
                "  step_desc   TEXT NOT NULL DEFAULT ''," +
                "  status      TEXT NOT NULL DEFAULT 'pending'," +
                "  error       TEXT," +
                "  stream_text TEXT DEFAULT ''," +
                "  result_json TEXT," +
                "  updated_at  TEXT," +
                "  PRIMARY KEY (task_id, step_index)" +
                ")"
            );

            // ── Step 0: 拆分 ──
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS step_split (" +
                "  id            INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  task_id       TEXT NOT NULL REFERENCES novel_tasks(id) ON DELETE CASCADE," +
                "  segment_index INTEGER NOT NULL," +
                "  content       TEXT," +
                "  created_at    TEXT NOT NULL" +
                ")"
            );
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_split_task ON step_split(task_id)");

            // ── Step 1: 角色提取 ──
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS step_characters (" +
                "  id          INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  task_id     TEXT NOT NULL REFERENCES novel_tasks(id) ON DELETE CASCADE," +
                "  char_index  INTEGER NOT NULL," +
                "  name        TEXT," +
                "  description TEXT," +
                "  created_at  TEXT NOT NULL" +
                ")"
            );
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_chars_task ON step_characters(task_id)");

            // ── Step 2: 三视图 ──
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS step_turnaround (" +
                "  id            INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  task_id       TEXT NOT NULL REFERENCES novel_tasks(id) ON DELETE CASCADE," +
                "  char_index    INTEGER NOT NULL," +
                "  name          TEXT," +
                "  front_prompt  TEXT," +
                "  back_prompt   TEXT," +
                "  side_prompt   TEXT," +
                "  combo_prompt  TEXT," +
                "  raw_text      TEXT," +
                "  created_at    TEXT NOT NULL" +
                ")"
            );
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_turn_task ON step_turnaround(task_id)");

            // ── Step 3: 分镜 ──
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS step_storyboard (" +
                "  id              INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  task_id         TEXT NOT NULL REFERENCES novel_tasks(id) ON DELETE CASCADE," +
                "  panel_index     INTEGER NOT NULL," +
                "  storyboard_text TEXT," +
                "  created_at      TEXT NOT NULL" +
                ")"
            );
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sb_task ON step_storyboard(task_id)");

            // ── Step 4: 改名 ──
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS step_rename (" +
                "  id            INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  task_id       TEXT NOT NULL REFERENCES novel_tasks(id) ON DELETE CASCADE," +
                "  mapping_index INTEGER NOT NULL," +
                "  name_from     TEXT," +
                "  name_to       TEXT," +
                "  created_at    TEXT NOT NULL" +
                ")"
            );
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_rename_task ON step_rename(task_id)");

            // ── Step 5: 组装提示词 ──
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS step_prompts (" +
                "  id              INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  task_id         TEXT NOT NULL REFERENCES novel_tasks(id) ON DELETE CASCADE," +
                "  prompt_index    INTEGER NOT NULL," +
                "  segment_text    TEXT," +
                "  storyboard_text TEXT," +
                "  full_prompt     TEXT," +
                "  raw_json        TEXT," +
                "  created_at      TEXT NOT NULL" +
                ")"
            );
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_prompts_task ON step_prompts(task_id)");

            // ── Step 6: 末张引流 ──
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS step_lastpage (" +
                "  id             INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  task_id        TEXT NOT NULL REFERENCES novel_tasks(id) ON DELETE CASCADE," +
                "  character_name TEXT," +
                "  title          TEXT," +
                "  prompt_text    TEXT," +
                "  created_at     TEXT NOT NULL" +
                ")"
            );
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lp_task ON step_lastpage(task_id)");

            log.info("Database schema ready (8 tables)");
        } catch (SQLException e) {
            log.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database init failed", e);
        }
    }

    private Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(dbUrl);
        c.setAutoCommit(true);
        return c;
    }

    // ══════════════════════════════════════════════════════════════════
    // Task CRUD
    // ══════════════════════════════════════════════════════════════════

    public String createTask(String title, String novelText, int segmentCount,
                              List<Map<String, String>> stepDefs) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String now = Instant.now().toString();

        lock.writeLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO novel_tasks (id, title, novel_text, segment_count, current_step, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, -1, 'CREATED', ?, ?)");
            ps.setString(1, taskId);
            ps.setString(2, title);
            ps.setString(3, novelText);
            ps.setInt(4, segmentCount);
            ps.setString(5, now);
            ps.setString(6, now);
            ps.executeUpdate();

            // Insert step status records
            PreparedStatement psStep = c.prepareStatement(
                "INSERT INTO step_status (task_id, step_index, step_id, step_name, step_icon, step_desc, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'pending')");
            for (int i = 0; i < stepDefs.size(); i++) {
                Map<String, String> def = stepDefs.get(i);
                psStep.setString(1, taskId);
                psStep.setInt(2, i);
                psStep.setString(3, def.get("id"));
                psStep.setString(4, def.get("name"));
                psStep.setString(5, def.getOrDefault("icon", ""));
                psStep.setString(6, def.getOrDefault("desc", ""));
                psStep.executeUpdate();
            }
            log.info("Created task [{}] \"{}\" ({} chars)", taskId, title, novelText.length());
            return taskId;
        } catch (SQLException e) {
            log.error("Failed to create task", e);
            throw new RuntimeException("Task creation failed", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateTaskMeta(String taskId, int currentStep, String status) {
        String now = Instant.now().toString();
        lock.writeLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement(
                "UPDATE novel_tasks SET current_step=?, status=?, updated_at=? WHERE id=?");
            ps.setInt(1, currentStep);
            ps.setString(2, status);
            ps.setString(3, now);
            ps.setString(4, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update task meta {}", taskId, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateStepStatus(String taskId, int stepIndex, String status,
                                  String error, String streamText, Object result) {
        String now = Instant.now().toString();
        lock.writeLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement(
                "UPDATE step_status SET status=?, error=?, stream_text=?, result_json=?, updated_at=? " +
                "WHERE task_id=? AND step_index=?");
            ps.setString(1, status);
            ps.setString(2, error);
            ps.setString(3, streamText != null ? streamText : "");
            ps.setString(4, result != null ? toJson(result) : null);
            ps.setString(5, now);
            ps.setString(6, taskId);
            ps.setInt(7, stepIndex);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update step status {}/{}", taskId, stepIndex, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Step data save methods — each step has its own table
    // ══════════════════════════════════════════════════════════════════

    /** Save split segments. Replaces all rows for this task. */
    public void saveSplitSegments(String taskId, List<String> segments) {
        String now = Instant.now().toString();
        lock.writeLock().lock();
        try (Connection c = connect()) {
            c.createStatement().execute("DELETE FROM step_split WHERE task_id='" + taskId + "'");
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO step_split (task_id, segment_index, content, created_at) VALUES (?,?,?,?)");
            for (int i = 0; i < segments.size(); i++) {
                ps.setString(1, taskId);
                ps.setInt(2, i);
                ps.setString(3, segments.get(i));
                ps.setString(4, now);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("saveSplitSegments failed", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Load split segments. */
    public List<String> loadSplitSegments(String taskId) {
        List<String> out = new ArrayList<>();
        lock.readLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT content FROM step_split WHERE task_id=? ORDER BY segment_index");
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getString("content"));
        } catch (SQLException e) {
            log.error("loadSplitSegments failed", e);
        } finally {
            lock.readLock().unlock();
        }
        return out;
    }

    /** Save extracted characters. */
    @SuppressWarnings("unchecked")
    public void saveCharacters(String taskId, List<Map<String, String>> characters) {
        String now = Instant.now().toString();
        lock.writeLock().lock();
        try (Connection c = connect()) {
            c.createStatement().execute("DELETE FROM step_characters WHERE task_id='" + taskId + "'");
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO step_characters (task_id, char_index, name, description, created_at) VALUES (?,?,?,?,?)");
            for (int i = 0; i < characters.size(); i++) {
                Map<String, String> ch = characters.get(i);
                ps.setString(1, taskId);
                ps.setInt(2, i);
                ps.setString(3, ch.get("name"));
                ps.setString(4, ch.get("description"));
                ps.setString(5, now);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("saveCharacters failed", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Load extracted characters. */
    public List<Map<String, String>> loadCharacters(String taskId) {
        List<Map<String, String>> out = new ArrayList<>();
        lock.readLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT name, description FROM step_characters WHERE task_id=? ORDER BY char_index");
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("name", rs.getString("name"));
                m.put("description", rs.getString("description"));
                out.add(m);
            }
        } catch (SQLException e) {
            log.error("loadCharacters failed", e);
        } finally {
            lock.readLock().unlock();
        }
        return out;
    }

    /** Save character turnarounds. */
    public void saveTurnarounds(String taskId, List<Map<String, String>> turnarounds) {
        String now = Instant.now().toString();
        lock.writeLock().lock();
        try (Connection c = connect()) {
            c.createStatement().execute("DELETE FROM step_turnaround WHERE task_id='" + taskId + "'");
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO step_turnaround (task_id, char_index, name, front_prompt, back_prompt, side_prompt, combo_prompt, raw_text, created_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?)");
            for (int i = 0; i < turnarounds.size(); i++) {
                Map<String, String> ta = turnarounds.get(i);
                ps.setString(1, taskId);
                ps.setInt(2, i);
                ps.setString(3, ta.get("name"));
                ps.setString(4, ta.get("frontPrompt"));
                ps.setString(5, ta.get("backPrompt"));
                ps.setString(6, ta.get("sidePrompt"));
                ps.setString(7, ta.get("comboPrompt"));
                ps.setString(8, ta.get("raw"));
                ps.setString(9, now);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("saveTurnarounds failed", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Load character turnarounds. */
    public List<Map<String, String>> loadTurnarounds(String taskId) {
        List<Map<String, String>> out = new ArrayList<>();
        lock.readLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT name, front_prompt, back_prompt, side_prompt, combo_prompt, raw_text " +
                "FROM step_turnaround WHERE task_id=? ORDER BY char_index");
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("name", rs.getString("name"));
                m.put("frontPrompt", rs.getString("front_prompt"));
                m.put("backPrompt", rs.getString("back_prompt"));
                m.put("sidePrompt", rs.getString("side_prompt"));
                m.put("comboPrompt", rs.getString("combo_prompt"));
                m.put("raw", rs.getString("raw_text"));
                out.add(m);
            }
        } catch (SQLException e) {
            log.error("loadTurnarounds failed", e);
        } finally {
            lock.readLock().unlock();
        }
        return out;
    }

    /** Save storyboard panels. */
    public void saveStoryboards(String taskId, List<String> storyboards) {
        String now = Instant.now().toString();
        lock.writeLock().lock();
        try (Connection c = connect()) {
            c.createStatement().execute("DELETE FROM step_storyboard WHERE task_id='" + taskId + "'");
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO step_storyboard (task_id, panel_index, storyboard_text, created_at) VALUES (?,?,?,?)");
            for (int i = 0; i < storyboards.size(); i++) {
                ps.setString(1, taskId);
                ps.setInt(2, i);
                ps.setString(3, storyboards.get(i));
                ps.setString(4, now);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("saveStoryboards failed", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Load storyboard panels. */
    public List<String> loadStoryboards(String taskId) {
        List<String> out = new ArrayList<>();
        lock.readLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT storyboard_text FROM step_storyboard WHERE task_id=? ORDER BY panel_index");
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getString("storyboard_text"));
        } catch (SQLException e) {
            log.error("loadStoryboards failed", e);
        } finally {
            lock.readLock().unlock();
        }
        return out;
    }

    /** Save name mappings. */
    public void saveNameMappings(String taskId, List<Map<String, String>> mappings) {
        String now = Instant.now().toString();
        lock.writeLock().lock();
        try (Connection c = connect()) {
            c.createStatement().execute("DELETE FROM step_rename WHERE task_id='" + taskId + "'");
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO step_rename (task_id, mapping_index, name_from, name_to, created_at) VALUES (?,?,?,?,?)");
            for (int i = 0; i < mappings.size(); i++) {
                Map<String, String> m = mappings.get(i);
                ps.setString(1, taskId);
                ps.setInt(2, i);
                ps.setString(3, m.get("from"));
                ps.setString(4, m.get("to"));
                ps.setString(5, now);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("saveNameMappings failed", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Load name mappings. */
    public List<Map<String, String>> loadNameMappings(String taskId) {
        List<Map<String, String>> out = new ArrayList<>();
        lock.readLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT name_from, name_to FROM step_rename WHERE task_id=? ORDER BY mapping_index");
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("from", rs.getString("name_from"));
                m.put("to", rs.getString("name_to"));
                out.add(m);
            }
        } catch (SQLException e) {
            log.error("loadNameMappings failed", e);
        } finally {
            lock.readLock().unlock();
        }
        return out;
    }

    /** Save assembled prompts. */
    @SuppressWarnings("unchecked")
    public void savePrompts(String taskId, List<Map<String, Object>> prompts) {
        String now = Instant.now().toString();
        lock.writeLock().lock();
        try (Connection c = connect()) {
            c.createStatement().execute("DELETE FROM step_prompts WHERE task_id='" + taskId + "'");
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO step_prompts (task_id, prompt_index, segment_text, storyboard_text, full_prompt, raw_json, created_at) " +
                "VALUES (?,?,?,?,?,?,?)");
            for (int i = 0; i < prompts.size(); i++) {
                Map<String, Object> p = prompts.get(i);
                ps.setString(1, taskId);
                ps.setInt(2, i);
                ps.setString(3, strVal(p.get("segment")));
                ps.setString(4, strVal(p.get("storyboard")));
                ps.setString(5, strVal(p.get("fullPrompt")));
                ps.setString(6, toJson(p));
                ps.setString(7, now);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("savePrompts failed", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Load assembled prompts. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadPrompts(String taskId) {
        List<Map<String, Object>> out = new ArrayList<>();
        lock.readLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT raw_json FROM step_prompts WHERE task_id=? ORDER BY prompt_index");
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String json = rs.getString("raw_json");
                if (json != null && !json.isEmpty()) {
                    out.add(mapper.readValue(json, Map.class));
                }
            }
        } catch (Exception e) {
            log.error("loadPrompts failed", e);
        } finally {
            lock.readLock().unlock();
        }
        return out;
    }

    /** Save last page prompt. */
    public void saveLastPage(String taskId, Map<String, Object> lastPage) {
        String now = Instant.now().toString();
        lock.writeLock().lock();
        try (Connection c = connect()) {
            c.createStatement().execute("DELETE FROM step_lastpage WHERE task_id='" + taskId + "'");
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO step_lastpage (task_id, character_name, title, prompt_text, created_at) VALUES (?,?,?,?,?)");
            ps.setString(1, taskId);
            ps.setString(2, strVal(lastPage.get("characterName")));
            ps.setString(3, strVal(lastPage.get("title")));
            ps.setString(4, strVal(lastPage.get("prompt")));
            ps.setString(5, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("saveLastPage failed", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Load last page prompt. */
    public Map<String, Object> loadLastPage(String taskId) {
        lock.readLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT character_name, title, prompt_text FROM step_lastpage WHERE task_id=? LIMIT 1");
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("characterName", rs.getString("character_name"));
                m.put("title", rs.getString("title"));
                m.put("prompt", rs.getString("prompt_text"));
                return m;
            }
        } catch (SQLException e) {
            log.error("loadLastPage failed", e);
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════
    // Load full task (all tables → one map)
    // ══════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public Map<String, Object> loadTask(String taskId) {
        lock.readLock().lock();
        try (Connection c = connect()) {
            // Task meta
            PreparedStatement ps = c.prepareStatement("SELECT * FROM novel_tasks WHERE id=?");
            ps.setString(1, taskId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            Map<String, Object> task = new LinkedHashMap<>();
            task.put("id", rs.getString("id"));
            task.put("title", rs.getString("title"));
            task.put("novelText", rs.getString("novel_text"));
            task.put("segmentCount", rs.getInt("segment_count"));
            task.put("currentStep", rs.getInt("current_step"));
            task.put("status", rs.getString("status"));
            task.put("createdAt", rs.getString("created_at"));
            task.put("updatedAt", rs.getString("updated_at"));

            // Step statuses
            PreparedStatement psSteps = c.prepareStatement(
                "SELECT * FROM step_status WHERE task_id=? ORDER BY step_index");
            psSteps.setString(1, taskId);
            ResultSet rsSteps = psSteps.executeQuery();
            List<Map<String, Object>> steps = new ArrayList<>();
            while (rsSteps.next()) {
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("index", rsSteps.getInt("step_index"));
                step.put("id", rsSteps.getString("step_id"));
                step.put("name", rsSteps.getString("step_name"));
                step.put("icon", rsSteps.getString("step_icon"));
                step.put("desc", rsSteps.getString("step_desc"));
                step.put("status", rsSteps.getString("status"));
                step.put("error", rsSteps.getString("error"));
                step.put("streamText", rsSteps.getString("stream_text"));
                String resultJson = rsSteps.getString("result_json");
                if (resultJson != null && !resultJson.isEmpty()) {
                    step.put("result", mapper.readValue(resultJson, Map.class));
                }
                steps.add(step);
            }
            task.put("steps", steps);

            // Pipeline data — loaded lazily by PipelineService
            // We just return the task meta + steps; data rows are loaded via the load* methods
            log.info("Loaded task [{}] \"{}\"", taskId, task.get("title"));
            return task;
        } catch (Exception e) {
            log.error("Failed to load task {}", taskId, e);
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // List projects
    // ══════════════════════════════════════════════════════════════════

    public List<Map<String, Object>> listProjects() {
        List<Map<String, Object>> projects = new ArrayList<>();
        lock.readLock().lock();
        try (Connection c = connect()) {
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT id, title, segment_count, current_step, status, novel_text, created_at, updated_at " +
                "FROM novel_tasks ORDER BY updated_at DESC");
            while (rs.next()) {
                String taskId = rs.getString("id");
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("id", taskId);
                p.put("title", rs.getString("title"));
                p.put("segmentCount", rs.getInt("segment_count"));
                p.put("currentStep", rs.getInt("current_step"));
                p.put("status", rs.getString("status"));
                p.put("novelLength", rs.getString("novel_text").length());
                p.put("createdAt", rs.getString("created_at"));
                p.put("updatedAt", rs.getString("updated_at"));

                // Count step statuses
                PreparedStatement psDone = c.prepareStatement(
                    "SELECT COUNT(*) FROM step_status WHERE task_id=? AND status='done'");
                psDone.setString(1, taskId);
                ResultSet rsDone = psDone.executeQuery();
                p.put("completedSteps", rsDone.next() ? rsDone.getInt(1) : 0);

                PreparedStatement psAll = c.prepareStatement(
                    "SELECT COUNT(*) FROM step_status WHERE task_id=?");
                psAll.setString(1, taskId);
                ResultSet rsAll = psAll.executeQuery();
                p.put("totalSteps", rsAll.next() ? rsAll.getInt(1) : 0);

                // Count rows in each data table
                p.put("splitCount", countRows(c, "step_split", taskId));
                p.put("charCount", countRows(c, "step_characters", taskId));
                p.put("turnCount", countRows(c, "step_turnaround", taskId));
                p.put("storyCount", countRows(c, "step_storyboard", taskId));
                p.put("renameCount", countRows(c, "step_rename", taskId));
                p.put("promptCount", countRows(c, "step_prompts", taskId));
                p.put("lastPageDone", countRows(c, "step_lastpage", taskId) > 0);

                projects.add(p);
            }
        } catch (SQLException e) {
            log.error("Failed to list projects", e);
        } finally {
            lock.readLock().unlock();
        }
        return projects;
    }

    private int countRows(Connection c, String table, String taskId) throws SQLException {
        PreparedStatement ps = c.prepareStatement(
            "SELECT COUNT(*) FROM " + table + " WHERE task_id=?");
        ps.setString(1, taskId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    public Map<String, Object> loadLatestTask() {
        lock.readLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM novel_tasks WHERE status != 'COMPLETED' ORDER BY updated_at DESC LIMIT 1");
            ResultSet rs = ps.executeQuery();
            String id = rs.next() ? rs.getString("id") : null;
            if (id == null) {
                ps = c.prepareStatement("SELECT id FROM novel_tasks ORDER BY updated_at DESC LIMIT 1");
                rs = ps.executeQuery();
                id = rs.next() ? rs.getString("id") : null;
            }
            if (id != null) return loadTask(id);
        } catch (SQLException e) {
            log.error("Failed to load latest task", e);
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public boolean deleteTask(String taskId) {
        lock.writeLock().lock();
        try (Connection c = connect()) {
            PreparedStatement ps = c.prepareStatement("DELETE FROM novel_tasks WHERE id=?");
            ps.setString(1, taskId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                log.info("Deleted task [{}] and all related data", taskId);
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Failed to delete task {}", taskId, e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String toJson(Object data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static String strVal(Object v) {
        return v != null ? v.toString() : "";
    }
}