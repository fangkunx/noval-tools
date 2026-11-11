package com.novel.comic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novel.comic.config.AppConfig;
import com.novel.comic.service.step.StepContext;
import com.novel.comic.service.step.StepStrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core pipeline service managing all 7 steps and their state.
 */
@Service
public class PipelineService {
    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    private final AppConfig.PipelineConfig config;
    private final AiService aiService;
    private final SseService sseService;
    private final ObjectMapper mapper;
    private final StepStrategyRegistry strategyRegistry;
    private final TaskStore taskStore;

    // ─── Pipeline State ─────────────────────────────────────────────
    private volatile String currentTaskId = null;
    private volatile String novelText = "";
    private volatile String novelTitle = "";
    private volatile int segmentCount;
    private volatile int currentStep = -1;

    private final List<StepState> steps = Collections.synchronizedList(new ArrayList<>());
    private final Set<Integer> running = ConcurrentHashMap.newKeySet();

    // Pipeline data fields
    private volatile List<String> segments = new ArrayList<>();
    private volatile List<Map<String, String>> characters = new ArrayList<>();
    private volatile List<Map<String, String>> characterTurnarounds = new ArrayList<>();
    private volatile List<String> storyboards = new ArrayList<>();
    private volatile String renamedText = "";
    private volatile List<Map<String, String>> nameMapping = new ArrayList<>();
    private volatile List<String> renamedSegments = new ArrayList<>();
    private volatile List<Map<String, Object>> finalPrompts = new ArrayList<>();
    private volatile Map<String, Object> lastPagePrompt = null;

    // ─── Step Definitions ──────────────────────────────────────────
    public static final List<Map<String, String>> STEP_DEFS = Arrays.asList(
            com.novel.comic.util.Maps.of("id", "split", "name", "拆分", "icon", "✂️", "desc", "将小说拆分成连续片段"),
            com.novel.comic.util.Maps.of("id", "characters", "name", "角色提取", "icon", "👤", "desc", "提取所有角色并生成形象描述"),
            com.novel.comic.util.Maps.of("id", "turnaround", "name", "角色三视图", "icon", "🎨", "desc", "为每个角色生成三视图垫图prompt"),
            com.novel.comic.util.Maps.of("id", "storyboard", "name", "分镜", "icon", "🎬", "desc", "绑定角色后创建漫画分镜"),
            com.novel.comic.util.Maps.of("id", "rename", "name", "角色改名", "icon", "✏️", "desc", "替换角色名字并同步称呼"),
            com.novel.comic.util.Maps.of("id", "prompts", "name", "组装提示词", "icon", "🔧", "desc", "组装前缀+分镜+后缀完整提示词"),
            com.novel.comic.util.Maps.of("id", "lastpage", "name", "末张引流", "icon", "📢", "desc", "生成漫画末张引流提示词")
    );

    public PipelineService(AppConfig.PipelineConfig config, AiService aiService,
                           SseService sseService, ObjectMapper mapper,
                           StepStrategyRegistry strategyRegistry,
                           TaskStore taskStore) {
        this.config = config;
        this.aiService = aiService;
        this.sseService = sseService;
        this.mapper = mapper;
        this.strategyRegistry = strategyRegistry;
        this.taskStore = taskStore;
        this.segmentCount = config.getDefaultSegments();
        resetPipeline();
        // Auto-resume the last active project on startup
        loadLatestTask();
    }

    // ─── State Management ───────────────────────────────────────────

    public synchronized void resetPipeline() {
        currentTaskId = null;
        novelText = "";
        novelTitle = "";
        segmentCount = config.getDefaultSegments();
        currentStep = -1;
        steps.clear();
        for (int i = 0; i < STEP_DEFS.size(); i++) {
            Map<String, String> def = STEP_DEFS.get(i);
            StepState s = new StepState();
            s.index = i;
            s.id = def.get("id");
            s.name = def.get("name");
            s.icon = def.get("icon");
            s.desc = def.get("desc");
            s.status = "pending";
            steps.add(s);
        }
        segments = new ArrayList<>();
        characters = new ArrayList<>();
        characterTurnarounds = new ArrayList<>();
        storyboards = new ArrayList<>();
        renamedText = "";
        nameMapping = new ArrayList<>();
        renamedSegments = new ArrayList<>();
        finalPrompts = new ArrayList<>();
        lastPagePrompt = null;
    }

    // ─── State Persistence ──────────────────────────────────────────

    /**
     * Save current pipeline state to SQLite.
     * Each step's data goes to its own dedicated table.
     * Called after init, after each step, and on error.
     */
    public synchronized void saveState() {
        if (currentTaskId == null) return;

        // Determine overall task status
        String taskStatus = "IN_PROGRESS";
        boolean allDone = steps.stream().allMatch(s -> "done".equals(s.status));
        if (allDone) taskStatus = "COMPLETED";

        // Update task metadata
        taskStore.updateTaskMeta(currentTaskId, currentStep, taskStatus);

        // Save each step's status
        for (StepState s : steps) {
            taskStore.updateStepStatus(currentTaskId, s.index, s.status, s.error,
                    sseService.getStreamText(s.index), s.result);
        }

        // Save step data to individual tables (only if data is non-empty)
        if (!segments.isEmpty()) taskStore.saveSplitSegments(currentTaskId, segments);
        if (!characters.isEmpty()) taskStore.saveCharacters(currentTaskId, characters);
        if (!characterTurnarounds.isEmpty()) taskStore.saveTurnarounds(currentTaskId, characterTurnarounds);
        if (!storyboards.isEmpty()) taskStore.saveStoryboards(currentTaskId, storyboards);
        if (!nameMapping.isEmpty()) taskStore.saveNameMappings(currentTaskId, nameMapping);
        if (!finalPrompts.isEmpty()) taskStore.savePrompts(currentTaskId, finalPrompts);
        if (lastPagePrompt != null && !lastPagePrompt.isEmpty()) taskStore.saveLastPage(currentTaskId, lastPagePrompt);
    }

    /**
     * Load a specific project from SQLite into current pipeline state.
     * Each step's data is loaded from its dedicated table.
     */
    @SuppressWarnings("unchecked")
    public synchronized boolean loadTask(String taskId) {
        Map<String, Object> task = taskStore.loadTask(taskId);
        if (task == null) return false;

        currentTaskId = taskId;
        novelTitle = str(task.get("title"), "未命名作品");
        novelText = str(task.get("novelText"), "");
        segmentCount = intVal(task.get("segmentCount"), config.getDefaultSegments());
        currentStep = intVal(task.get("currentStep"), -1);

        // Load step data from individual tables
        segments = taskStore.loadSplitSegments(taskId);
        characters = taskStore.loadCharacters(taskId);
        characterTurnarounds = taskStore.loadTurnarounds(taskId);
        storyboards = taskStore.loadStoryboards(taskId);
        nameMapping = taskStore.loadNameMappings(taskId);
        finalPrompts = taskStore.loadPrompts(taskId);

        Map<String, Object> lp = taskStore.loadLastPage(taskId);
        lastPagePrompt = lp;

        // Rebuild renamedText and renamedSegments from loaded data
        if (!nameMapping.isEmpty()) {
            StringBuilder rt = new StringBuilder();
            for (Map<String, String> m : nameMapping) {
                rt.append(m.get("from")).append(" | ").append(m.get("to")).append("\n");
            }
            renamedText = rt.toString();

            if (!segments.isEmpty()) {
                List<String> rs = new ArrayList<>();
                for (String seg : segments) {
                    String r = seg;
                    for (Map<String, String> mp : nameMapping) {
                        String from = mp.get("from");
                        String to = mp.get("to");
                        if (from != null && to != null && !from.equals(to)) {
                            r = r.replaceAll(java.util.regex.Pattern.quote(from), to);
                        }
                    }
                    rs.add(r);
                }
                renamedSegments = rs;
            }
        }

        // Restore step statuses
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> savedSteps =
                (List<Map<String, Object>>) task.get("steps");
        if (savedSteps != null) {
            for (Map<String, Object> saved : savedSteps) {
                int idx = intVal(saved.get("index"), -1);
                if (idx < 0 || idx >= steps.size()) continue;
                steps.get(idx).status = str(saved.get("status"), "pending");
                steps.get(idx).error = str(saved.get("error"), null);
                steps.get(idx).result = mapVal(saved.get("result"));
                String st = str(saved.get("streamText"), "");
                if (st != null && !st.isEmpty()) {
                    sseService.clearStreamText(idx);
                    sseService.appendStreamText(idx, st);
                }
            }
        }
        log.info("Restored task [{}] \"{}\" — segments={}, chars={}, turnarounds={}, storyboards={}, mappings={}, prompts={}, lastpage={}",
                taskId, novelTitle,
                segments.size(), characters.size(), characterTurnarounds.size(),
                storyboards.size(), nameMapping.size(), finalPrompts.size(),
                lastPagePrompt != null);
        return true;
    }

    /**
     * Auto-load the most recently active project on startup.
     */
    public synchronized void loadLatestTask() {
        Map<String, Object> task = taskStore.loadLatestTask();
        if (task != null) {
            String taskId = (String) task.get("id");
            loadTask(taskId);
        }
    }

    /**
     * Delete the current project from the database and reset in-memory state.
     */
    public synchronized void clearStateFile() {
        if (currentTaskId != null) {
            taskStore.deleteTask(currentTaskId);
        }
        resetPipeline();
    }

    // ─── Getters ─────────────────────────────────────────────────────

    public List<StepState> getSteps() { return new ArrayList<>(steps); }
    public StepState getStep(int idx) { return idx >= 0 && idx < steps.size() ? steps.get(idx) : null; }
    public StepState getStepById(String id) {
        return steps.stream().filter(s -> s.id.equals(id)).findFirst().orElse(null);
    }
    public boolean isRunning(int idx) { return running.contains(idx); }
    public Set<Integer> getRunning() { return new HashSet<>(running); }
    public String getNovelTitle() { return novelTitle; }
    public int getNovelLength() { return novelText.length(); }
    public int getSegmentCount() { return segmentCount; }
    public int getCurrentStep() { return currentStep; }
    public String getCurrentTaskId() { return currentTaskId; }

    // ── Shared state accessors (for step strategies) ──────────────

    public String getNovelText() { return novelText; }
    public List<String> getSegments() { return segments; }
    public void setSegments(List<String> v) { this.segments = v; }
    public List<Map<String, String>> getCharacters() { return characters; }
    public void setCharacters(List<Map<String, String>> v) { this.characters = v; }
    public List<Map<String, String>> getCharacterTurnarounds() { return characterTurnarounds; }
    public void setCharacterTurnarounds(List<Map<String, String>> v) { this.characterTurnarounds = v; }
    public List<String> getStoryboards() { return storyboards; }
    public void setStoryboards(List<String> v) { this.storyboards = v; }
    public List<Map<String, String>> getNameMapping() { return nameMapping; }
    public void setNameMapping(List<Map<String, String>> v) { this.nameMapping = v; }
    public String getRenamedText() { return renamedText; }
    public void setRenamedText(String v) { this.renamedText = v; }
    public List<String> getRenamedSegments() { return renamedSegments; }
    public void setRenamedSegments(List<String> v) { this.renamedSegments = v; }
    public List<Map<String, Object>> getFinalPrompts() { return finalPrompts; }
    public void setFinalPrompts(List<Map<String, Object>> v) { this.finalPrompts = v; }
    public Map<String, Object> getLastPagePrompt() { return lastPagePrompt; }
    public void setLastPagePrompt(Map<String, Object> v) { this.lastPagePrompt = v; }
    public void setSegmentCount(int v) { this.segmentCount = v; }

    // ── Package-private mutators for step strategy framework ──────

    void setCurrentStep(int v) { currentStep = v; }
    Set<Integer> runningSet() { return running; }
    void saveStateInternal() { saveState(); }
    void emitInternal(int idx, String event, Object data) { emit(idx, event, data); }

    // ─── Public API ──────────────────────────────────────────────────

    public Map<String, Object> init(String text, int segCount, String title) {
        int count = Math.max(1, Math.min(segCount, config.getMaxSegments()));
        // If there's already a task loaded with the same text, don't create a new one
        // Otherwise, always create a fresh project in the database
        resetPipeline();
        novelText = text.trim();
        novelTitle = title != null && !title.isEmpty() ? title : "未命名作品";
        segmentCount = count;

        // Create project in SQLite
        currentTaskId = taskStore.createTask(novelTitle, novelText, segmentCount, STEP_DEFS);

        int chars = novelText.length();
        int estMax = chars / 20;
        saveState();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("taskId", currentTaskId);
        resp.put("segmentCount", count);
        resp.put("novelLength", chars);
        resp.put("title", novelTitle);
        resp.put("estimatedMaxSegments", estMax);
        resp.put("warning", count > estMax
                ? "文本" + chars + "字，预估最多" + estMax + "段。当前" + count + "段过多。" : null);

        List<Map<String, Object>> stepInfos = new ArrayList<>();
        for (StepState s : steps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", s.index); m.put("id", s.id); m.put("name", s.name);
            m.put("icon", s.icon); m.put("desc", s.desc); m.put("status", s.status);
            stepInfos.add(m);
        }
        resp.put("steps", stepInfos);
        return resp;
    }

    public Map<String, Object> rerunFrom(int stepIndex) {
        for (int i = stepIndex; i < steps.size(); i++) {
            StepState s = steps.get(i);
            s.status = "pending";
            s.error = null;
            s.result = null;
            sseService.clearStreamText(i);
        }
        // Clear downstream data
        if (stepIndex <= 1) { characterTurnarounds.clear(); storyboards.clear(); renamedSegments.clear(); finalPrompts.clear(); lastPagePrompt = null; }
        if (stepIndex <= 2) { storyboards.clear(); renamedSegments.clear(); finalPrompts.clear(); lastPagePrompt = null; }
        if (stepIndex <= 3) { renamedSegments.clear(); finalPrompts.clear(); lastPagePrompt = null; }
        if (stepIndex <= 4) { nameMapping.clear(); renamedSegments.clear(); finalPrompts.clear(); lastPagePrompt = null; }
        if (stepIndex <= 5) { finalPrompts.clear(); lastPagePrompt = null; }
        if (stepIndex <= 6) { lastPagePrompt = null; }

        saveState();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("rerunFrom", stepIndex);
        List<Map<String, String>> si = new ArrayList<>();
        for (StepState s : steps) {
            si.add(com.novel.comic.util.Maps.of("index", String.valueOf(s.index), "id", s.id, "status", s.status));
        }
        resp.put("steps", si);
        return resp;
    }

    // ─── Step Execution ──────────────────────────────────────────────

    @Async
    public void executeStep(int stepIndex) {
        if (!running.add(stepIndex)) return;

        StepState step = steps.get(stepIndex);
        step.status = "running";
        step.error = null;
        sseService.clearStreamText(stepIndex);
        currentStep = stepIndex;

        emit(stepIndex, "status", com.novel.comic.util.Maps.of(
                "status", "running", "step", stepIndex, "name", step.name));

        try {
            StepContext ctx = new StepContext(this, stepIndex,
                    aiService, sseService, config, mapper);
            strategyRegistry.resolve(step.id).execute(ctx);

            step.status = "done";
            saveState();
            emit(stepIndex, "done", com.novel.comic.util.Maps.of(
                    "step", stepIndex, "result",
                    (Object) (step.result != null ? step.result : new HashMap<>())));
        } catch (Exception e) {
            step.status = "error";
            step.error = e.getMessage();
            saveState();
            emit(stepIndex, "error", com.novel.comic.util.Maps.of(
                    "step", stepIndex, "message", e.getMessage()));
            log.error("Step {} error: {}", step.id, e.getMessage(), e);
        } finally {
            running.remove(stepIndex);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private void emit(int stepIndex, String event, Object data) {
        sseService.broadcast(stepIndex, event, data);
    }

    public Map<String, Object> buildDownloadData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", novelTitle);
        data.put("segmentCount", segmentCount);
        data.put("generatedAt", Instant.now().toString());
        data.put("segments", segments);
        data.put("characters", characters);
        data.put("characterTurnarounds", characterTurnarounds);
        data.put("storyboards", storyboards);
        data.put("nameMapping", nameMapping);
        data.put("renamedSegments", renamedSegments);
        data.put("finalPrompts", finalPrompts);
        data.put("lastPagePrompt", lastPagePrompt);
        return data;
    }

    public Map<String, Object> buildStateResponse() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("taskId", currentTaskId);
        resp.put("hasState", !novelText.isEmpty());
        resp.put("title", novelTitle);
        resp.put("segmentCount", segmentCount);
        List<Map<String, Object>> si = new ArrayList<>();
        for (StepState s : steps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", s.index); m.put("id", s.id); m.put("name", s.name);
            m.put("icon", s.icon); m.put("desc", s.desc);
            m.put("status", s.status); m.put("error", s.error);
            si.add(m);
        }
        resp.put("steps", si);
        resp.put("completedSteps", si.stream().filter(s -> "done".equals(s.get("status"))).count());
        resp.put("totalSteps", si.size());
        return resp;
    }

    public Map<String, Object> buildStatusResponse() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("taskId", currentTaskId);
        resp.put("currentStep", currentStep);
        resp.put("segmentCount", segmentCount);
        resp.put("novelLength", novelText.length());
        resp.put("title", novelTitle);
        resp.put("running", new ArrayList<>(running));
        List<Map<String, Object>> si = new ArrayList<>();
        for (StepState s : steps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", s.index); m.put("id", s.id); m.put("name", s.name);
            m.put("icon", s.icon); m.put("desc", s.desc);
            m.put("status", s.status); m.put("error", s.error);
            m.put("streamText", sseService.getStreamText(s.index));
            si.add(m);
        }
        resp.put("steps", si);
        return resp;
    }

    // ─── Type-safe extraction helpers ────────────────────────────────

    @SuppressWarnings("unchecked")
    private static String str(Object v, String def) {
        return v instanceof String ? (String) v : def;
    }

    private static int intVal(Object v, int def) {
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(Object v) {
        if (v instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object item : (List<Object>) v) out.add(item != null ? item.toString() : "");
            return out;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> mapList(Object v) {
        List<Map<String, String>> out = new ArrayList<>();
        if (v instanceof List) {
            for (Object item : (List<Object>) v) {
                if (item instanceof Map) {
                    Map<String, String> m = new LinkedHashMap<>();
                    ((Map<String, Object>) item).forEach((k, val) -> m.put(k, val != null ? val.toString() : ""));
                    out.add(m);
                }
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objList(Object v) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (v instanceof List) {
            for (Object item : (List<Object>) v) {
                if (item instanceof Map) out.add((Map<String, Object>) item);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapVal(Object v) {
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    // ─── Step State Inner Class ──────────────────────────────────────

    public static class StepState {
        public int index;
        public String id;
        public String name;
        public String icon;
        public String desc;
        public String status = "pending";
        public String error;
        public Map<String, Object> result;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", index); m.put("id", id); m.put("name", name);
            m.put("icon", icon); m.put("desc", desc);
            m.put("status", status); m.put("error", error);
            return m;
        }
    }
}