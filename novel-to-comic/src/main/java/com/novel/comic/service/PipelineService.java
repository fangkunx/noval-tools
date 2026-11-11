package com.novel.comic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novel.comic.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // ─── Pipeline State ─────────────────────────────────────────────
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
                           SseService sseService, ObjectMapper mapper) {
        this.config = config;
        this.aiService = aiService;
        this.sseService = sseService;
        this.mapper = mapper;
        this.segmentCount = config.getDefaultSegments();
        resetPipeline();
    }

    // ─── State Management ───────────────────────────────────────────

    public synchronized void resetPipeline() {
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

    public synchronized void saveState() {
        try {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("novelText", novelText);
            state.put("novelTitle", novelTitle);
            state.put("segmentCount", segmentCount);
            state.put("currentStep", currentStep);
            state.put("savedAt", Instant.now().toString());

            List<Map<String, Object>> stepList = new ArrayList<>();
            for (StepState s : steps) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("index", s.index);
                m.put("id", s.id);
                m.put("name", s.name);
                m.put("icon", s.icon);
                m.put("desc", s.desc);
                m.put("status", s.status);
                m.put("error", s.error);
                m.put("streamText", sseService.getStreamText(s.index));
                m.put("result", s.result);
                stepList.add(m);
            }
            state.put("steps", stepList);
            state.put("segments", segments);
            state.put("characters", characters);
            state.put("characterTurnarounds", characterTurnarounds);
            state.put("storyboards", storyboards);
            state.put("renamedText", renamedText);
            state.put("nameMapping", nameMapping);
            state.put("renamedSegments", renamedSegments);
            state.put("finalPrompts", finalPrompts);
            state.put("lastPagePrompt", lastPagePrompt);

            Files.write(config.getStateFilePath(),
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(state)
                            .getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to save pipeline state: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized boolean loadState() {
        Path fp = config.getStateFilePath();
        if (!Files.exists(fp)) return false;
        try {
            String json = new String(Files.readAllBytes(fp), StandardCharsets.UTF_8);
            Map<String, Object> state = mapper.readValue(json, Map.class);

            novelText = str(state.get("novelText"), "");
            novelTitle = str(state.get("novelTitle"), "");
            segmentCount = intVal(state.get("segmentCount"), config.getDefaultSegments());
            currentStep = intVal(state.get("currentStep"), -1);
            segments = list(state.get("segments"));
            characters = mapList(state.get("characters"));
            characterTurnarounds = mapList(state.get("characterTurnarounds"));
            storyboards = list(state.get("storyboards"));
            renamedText = str(state.get("renamedText"), "");
            nameMapping = mapList(state.get("nameMapping"));
            renamedSegments = list(state.get("renamedSegments"));
            finalPrompts = objList(state.get("finalPrompts"));
            lastPagePrompt = mapVal(state.get("lastPagePrompt"));

            // Restore steps
            List<Map<String, Object>> savedSteps = objList(state.get("steps"));
            if (savedSteps != null && savedSteps.size() == steps.size()) {
                for (int i = 0; i < steps.size(); i++) {
                    Map<String, Object> saved = savedSteps.get(i);
                    steps.get(i).status = str(saved.get("status"), "pending");
                    steps.get(i).error = str(saved.get("error"), null);
                    steps.get(i).result = mapVal(saved.get("result"));
                    String st = str(saved.get("streamText"), "");
                    if (st != null && !st.isEmpty()) {
                        sseService.appendStreamText(i, st);
                    }
                }
            }
            log.info("📂 已恢复上次任务: \"{}\"", novelTitle);
            return true;
        } catch (Exception e) {
            log.error("Failed to load pipeline state: {}", e.getMessage());
            return false;
        }
    }

    public void clearStateFile() {
        try { Files.deleteIfExists(config.getStateFilePath()); } catch (Exception ignored) {}
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

    // ─── Public API ──────────────────────────────────────────────────

    public Map<String, Object> init(String text, int segCount, String title) {
        int count = Math.max(1, Math.min(segCount, config.getMaxSegments()));
        resetPipeline();
        novelText = text.trim();
        novelTitle = title != null && !title.isEmpty() ? title : "未命名作品";
        segmentCount = count;
        int chars = novelText.length();
        int estMax = chars / 20;
        saveState();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
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

        emit(stepIndex, "status", com.novel.comic.util.Maps.of("status", "running", "step", stepIndex, "name", step.name));

        try {
            switch (step.id) {
                case "split": doSplit(stepIndex); break;
                case "characters": doCharacters(stepIndex); break;
                case "turnaround": doTurnaround(stepIndex); break;
                case "storyboard": doStoryboard(stepIndex); break;
                case "rename": doRename(stepIndex); break;
                case "prompts": doPrompts(stepIndex); break;
                case "lastpage": doLastPage(stepIndex); break;
            }
            step.status = "done";
            saveState();
            emit(stepIndex, "done", com.novel.comic.util.Maps.of("step", stepIndex, "result", (Object) (step.result != null ? step.result : new HashMap<>())));
        } catch (Exception e) {
            step.status = "error";
            step.error = e.getMessage();
            saveState();
            emit(stepIndex, "error", com.novel.comic.util.Maps.of("step", stepIndex, "message", e.getMessage()));
            log.error("Step {} error: {}", step.id, e.getMessage(), e);
        } finally {
            running.remove(stepIndex);
        }
    }

    // ─── Step: Split ────────────────────────────────────────────────

    private void doSplit(int idx) throws Exception {
        emit(idx, "progress", com.novel.comic.util.Maps.of("message", "AI 正在分析小说并拆分...", "done", 0, "total", 1));

        String prompt = aiService.loadPrompt("split", com.novel.comic.util.Maps.of(
                "SEGMENT_COUNT", String.valueOf(segmentCount),
                "NOVEL_TEXT", novelText
        ));

        String raw = aiService.callAISmart(
                "你是专业的漫画分镜导演。只输出拆分结果，每行一段，按编号输出，不要任何多余内容。",
                prompt,
                new AiService.StreamCallback() {
                    @Override public void onChunk(String text) {
                        emit(idx, "chunk", com.novel.comic.util.Maps.of("text", text));
                        sseService.appendStreamText(idx, text);
                    }
                    @Override public void onChunk(String text, int index) { onChunk(text); }
                    @Override public void onStatus(String message) { emit(idx, "status", com.novel.comic.util.Maps.of("message", message)); }
                    @Override public void onProgress(String message, int done, int total) {}
                },
                false, 16000, 0.3
        );

        List<String> segs = new ArrayList<>();
        Pattern p = Pattern.compile("^\\d+[\\.\\、\\)）]\\s*(.+)");
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            Matcher m = p.matcher(line);
            segs.add(m.find() ? m.group(1).trim() : line);
        }
        segments = segs.isEmpty() ? Arrays.asList(raw.split("\n")) : segs;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "segments");
        result.put("count", segments.size());
        result.put("segments", segments);
        steps.get(idx).result = result;
        emit(idx, "result", result);
        saveOutputFile("split", com.novel.comic.util.Maps.of("segments", segments));
    }

    // ─── Step: Characters ───────────────────────────────────────────

    private void doCharacters(int idx) throws Exception {
        emit(idx, "progress", com.novel.comic.util.Maps.of("message", "AI 正在提取角色...", "done", 0, "total", 1));

        String prompt = aiService.loadPrompt("character-extract", com.novel.comic.util.Maps.of("NOVEL_TEXT", novelText));
        String raw = aiService.callAISmart(
                "你是经验丰富的漫画人物设计大师。只输出角色信息，格式：角色名：描述。",
                prompt,
                new AiService.StreamCallback() {
                    @Override public void onChunk(String text) {
                        emit(idx, "chunk", com.novel.comic.util.Maps.of("text", text));
                        sseService.appendStreamText(idx, text);
                    }
                    @Override public void onChunk(String text, int index) { onChunk(text); }
                    @Override public void onStatus(String message) { emit(idx, "status", com.novel.comic.util.Maps.of("message", message)); }
                    @Override public void onProgress(String message, int done, int total) {}
                },
                false, 4096, 0.3
        );

        List<Map<String, String>> chars = new ArrayList<>();
        Pattern p = Pattern.compile("^(.+?)[：:]\\s*(.+)");
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            Matcher m = p.matcher(line);
            if (m.find()) {
                chars.add(com.novel.comic.util.Maps.of("name", m.group(1).trim(), "description", m.group(2).trim()));
            }
        }
        characters = chars;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "characters");
        result.put("count", characters.size());
        result.put("characters", characters);
        steps.get(idx).result = result;
        emit(idx, "result", result);
        saveOutputFile("characters", com.novel.comic.util.Maps.of("characters", characters));
    }

    // ─── Step: Turnaround ───────────────────────────────────────────

    private void doTurnaround(int idx) throws Exception {
        if (characters.isEmpty()) throw new RuntimeException("请先执行角色提取");

        StringBuilder charList = new StringBuilder();
        for (Map<String, String> c : characters) {
            charList.append("**").append(c.get("name")).append("**：").append(c.get("description")).append("\n");
        }

        emit(idx, "progress", com.novel.comic.util.Maps.of("message", "AI 正在生成角色三视图...", "done", 0, "total", 1));

        String prompt = aiService.loadPrompt("character-turnaround", com.novel.comic.util.Maps.of("CHARACTER_LIST", charList.toString()));
        String raw = aiService.callAISmart(
                "你是Q版火柴人漫画角色设计师。为每个角色生成三视图prompt，用===分隔。",
                prompt,
                new AiService.StreamCallback() {
                    @Override public void onChunk(String text) {
                        emit(idx, "chunk", com.novel.comic.util.Maps.of("text", text));
                        sseService.appendStreamText(idx, text);
                    }
                    @Override public void onChunk(String text, int index) { onChunk(text); }
                    @Override public void onStatus(String message) { emit(idx, "status", com.novel.comic.util.Maps.of("message", message)); }
                    @Override public void onProgress(String message, int done, int total) {}
                },
                false, 4096, 0.7
        );

        List<Map<String, String>> turnarounds = new ArrayList<>();
        String[] sections = raw.split("===+\\s*");
        for (String section : sections) {
            section = section.trim();
            if (section.isEmpty()) continue;
            String[] lines = section.split("\n");
            String name = lines[0].trim().replaceAll("[：:]", "").trim();
            String body = String.join("\n", Arrays.copyOfRange(lines, 1, lines.length)).trim();
            if (name.isEmpty() || body.isEmpty()) continue;

            Map<String, String> ta = new LinkedHashMap<>();
            ta.put("name", name);
            ta.put("frontPrompt", extractView(body, "【正面视图[^】]*】"));
            ta.put("backPrompt", extractView(body, "【背面视图[^】]*】"));
            ta.put("sidePrompt", extractView(body, "【侧面视图[^】]*】"));
            String combo = extractView(body, "【综合三视图[^】]*】");
            ta.put("comboPrompt", combo.isEmpty() ? body : combo);
            ta.put("raw", body);
            turnarounds.add(ta);
        }
        characterTurnarounds = turnarounds;

        List<String> charNames = new ArrayList<>();
        for (Map<String, String> c : characters) charNames.add(c.get("name"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "turnarounds");
        result.put("count", turnarounds.size());
        result.put("characters", charNames);
        result.put("turnarounds", turnarounds);
        steps.get(idx).result = result;
        emit(idx, "result", result);
        saveOutputFile("turnaround", com.novel.comic.util.Maps.of("turnarounds", turnarounds, "characters", characters));
    }

    private String extractView(String body, String labelPattern) {
        Pattern p = Pattern.compile(labelPattern + "\\s*([\\s\\S]*?)(?=【|---|$)");
        Matcher m = p.matcher(body);
        return m.find() ? m.group(1).trim() : "";
    }

    // ─── Step: Storyboard ───────────────────────────────────────────

    private void doStoryboard(int idx) throws Exception {
        if (segments.isEmpty()) throw new RuntimeException("请先执行拆分");

        StringBuilder charRefs = new StringBuilder();
        if (!characters.isEmpty()) {
            for (Map<String, String> c : characters) {
                charRefs.append("- **").append(c.get("name")).append("**：").append(c.get("description"));
                Map<String, String> ta = characterTurnarounds.stream()
                        .filter(t -> t.get("name").equals(c.get("name"))).findFirst().orElse(null);
                if (ta != null && ta.containsKey("comboPrompt") && !ta.get("comboPrompt").isEmpty()) {
                    String cp = ta.get("comboPrompt");
                    if (cp.length() > 200) cp = cp.substring(0, 200);
                    charRefs.append("\n  三视图参考: ").append(cp);
                }
                charRefs.append("\n");
            }
        }
        String charRefStr = charRefs.length() > 0 ? charRefs.toString() : "(无已提取角色)";

        List<String> sbList = new ArrayList<>(Collections.nCopies(segments.size(), ""));
        int[] doneCount = {0};
        int total = segments.size();
        emit(idx, "progress", com.novel.comic.util.Maps.of("message", "分镜处理 0/" + total, "done", 0, "total", total));

        int concurrency = config.getConcurrency();
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            final int segIdx = i;
            final String charRefFinal = charRefStr;
            futures.add(executor.submit(() -> {
                try {
                    String prompt = aiService.loadPrompt("storyboard", com.novel.comic.util.Maps.of(
                            "SEGMENT_TEXT", segments.get(segIdx),
                            "CHARACTER_REFS", charRefFinal
                    ));
                    String text = aiService.callAISmart(
                            "你是专业的Q版漫画分镜导演。严格按照Q版火柴人风格创作分镜。",
                            prompt,
                            new AiService.StreamCallback() {
                                @Override public void onChunk(String text) {
                                    emit(idx, "chunk", com.novel.comic.util.Maps.of("text", text, "index", segIdx));
                                    sseService.appendStreamText(idx, text);
                                }
                                @Override public void onChunk(String text, int index) { onChunk(text); }
                                @Override public void onStatus(String message) { emit(idx, "status", com.novel.comic.util.Maps.of("message", message)); }
                                @Override public void onProgress(String message, int done, int total) {}
                            },
                            false, 4096, 0.7
                    );
                    synchronized (sbList) { sbList.set(segIdx, text); }
                } catch (Exception e) {
                    synchronized (sbList) { sbList.set(segIdx, "[失败: " + e.getMessage() + "]"); }
                }
                synchronized (doneCount) {
                    doneCount[0]++;
                    emit(idx, "progress", com.novel.comic.util.Maps.of("message", "分镜 " + doneCount[0] + "/" + total, "done", doneCount[0], "total", total));
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(600, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        executor.shutdown();
        storyboards = sbList;

        long cnt = storyboards.stream().filter(s -> s != null && !s.startsWith("[失败")).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "storyboards");
        result.put("count", (int) cnt);
        result.put("total", total);
        result.put("storyboards", storyboards);
        steps.get(idx).result = result;
        emit(idx, "result", result);
        saveOutputFile("storyboard", com.novel.comic.util.Maps.of("storyboards", storyboards));
    }

    // ─── Step: Rename ───────────────────────────────────────────────

    private void doRename(int idx) throws Exception {
        String text = !segments.isEmpty() ? String.join("\n\n", segments) : novelText;
        String prompt = aiService.loadPrompt("character-rename", com.novel.comic.util.Maps.of("NOVEL_TEXT", text));

        emit(idx, "progress", com.novel.comic.util.Maps.of("message", "AI 正在替换角色名字...", "done", 0, "total", 1));

        String raw = aiService.callAISmart(
                "你是专业的文字编辑。替换角色名字，保留姓氏，修改名字。列出对照表。",
                prompt,
                new AiService.StreamCallback() {
                    @Override public void onChunk(String text) {
                        emit(idx, "chunk", com.novel.comic.util.Maps.of("text", text));
                        sseService.appendStreamText(idx, text);
                    }
                    @Override public void onChunk(String text, int index) { onChunk(text); }
                    @Override public void onStatus(String message) { emit(idx, "status", com.novel.comic.util.Maps.of("message", message)); }
                    @Override public void onProgress(String message, int done, int total) {}
                },
                false, 16000, 0.3
        );

        // Parse mapping
        List<Map<String, String>> mapping = new ArrayList<>();
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.contains("|")) {
                String[] cells = line.split("\\|");
                if (cells.length >= 2) {
                    mapping.add(com.novel.comic.util.Maps.of("from", cells[0].trim(), "to", cells[1].trim()));
                }
            }
        }
        if (mapping.isEmpty()) {
            Pattern arrow = Pattern.compile("(.+?)\\s*[→>]\\s*(.+)");
            Matcher m = arrow.matcher(raw);
            while (m.find()) {
                mapping.add(com.novel.comic.util.Maps.of("from", m.group(1).trim(), "to", m.group(2).trim()));
            }
        }

        nameMapping = mapping;
        renamedText = raw;

        if (!mapping.isEmpty() && !segments.isEmpty()) {
            List<String> rs = new ArrayList<>();
            for (String seg : segments) {
                String r = seg;
                for (Map<String, String> mp : mapping) {
                    String from = mp.get("from");
                    String to = mp.get("to");
                    if (from != null && to != null && !from.equals(to)) {
                        r = r.replaceAll(Pattern.quote(from), to);
                    }
                }
                rs.add(r);
            }
            renamedSegments = rs;
        } else {
            renamedSegments = new ArrayList<>(segments);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "rename");
        result.put("mapping", mapping);
        result.put("renamedSegments", renamedSegments);
        result.put("preview", raw.length() > 2000 ? raw.substring(0, 2000) : raw);
        steps.get(idx).result = result;
        emit(idx, "result", result);
        saveOutputFile("rename", com.novel.comic.util.Maps.of("mapping", mapping, "renamedSegments", renamedSegments));
    }

    // ─── Step: Prompts ──────────────────────────────────────────────

    private void doPrompts(int idx) throws Exception {
        String prefix = aiService.loadPrompt("prefix", null);
        String suffix = aiService.loadPrompt("suffix", null);

        StringBuilder charRefBlock = new StringBuilder();
        if (!characterTurnarounds.isEmpty()) {
            charRefBlock.append("\n\n【角色垫图参考】\n");
            for (Map<String, String> ta : characterTurnarounds) {
                String combo = ta.get("comboPrompt");
                if (combo != null && !combo.isEmpty()) {
                    if (combo.length() > 300) combo = combo.substring(0, 300);
                    charRefBlock.append(ta.get("name")).append(": ").append(combo).append("\n");
                }
            }
        }

        List<String> segs = !renamedSegments.isEmpty() ? renamedSegments : segments;
        int total = Math.max(segs.size(), storyboards.size());
        List<Map<String, Object>> prompts = new ArrayList<>();

        emit(idx, "progress", com.novel.comic.util.Maps.of("message", "组装 0/" + total, "done", 0, "total", total));
        for (int i = 0; i < total; i++) {
            String sb = i < storyboards.size() ? storyboards.get(i) : "";
            String fp = prefix + charRefBlock + "\n\n" + (sb != null ? sb : "") + "\n\n" + suffix;

            Map<String, Object> promptEntry = new LinkedHashMap<>();
            promptEntry.put("index", i + 1);
            promptEntry.put("segment", i < segs.size() ? segs.get(i) : "");
            promptEntry.put("storyboard", sb);
            promptEntry.put("prefix", prefix);
            promptEntry.put("suffix", suffix);
            promptEntry.put("charRefBlock", charRefBlock.toString());
            promptEntry.put("fullPrompt", fp);
            prompts.add(promptEntry);

            emit(idx, "progress", com.novel.comic.util.Maps.of("message", "组装 " + (i + 1) + "/" + total, "done", i + 1, "total", total));
        }

        finalPrompts = prompts;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "prompts");
        result.put("count", prompts.size());
        result.put("prompts", prompts);
        steps.get(idx).result = result;
        emit(idx, "result", result);
        saveOutputFile("prompts", com.novel.comic.util.Maps.of("prompts", prompts));
    }

    // ─── Step: Last Page ────────────────────────────────────────────

    private void doLastPage(int idx) throws Exception {
        String primaryChar = "主角";
        if (!characters.isEmpty()) {
            String firstName = characters.get(0).get("name");
            for (Map<String, String> mp : nameMapping) {
                if (firstName != null && firstName.contains(mp.get("from"))) {
                    primaryChar = mp.get("to");
                    break;
                }
            }
            if (primaryChar.equals("主角")) primaryChar = firstName;
        }

        String title = novelTitle.isEmpty() ? "未命名作品" : novelTitle;
        String prompt = aiService.loadPrompt("last-page", com.novel.comic.util.Maps.of(
                "TITLE", title, "CHARACTER_NAME", primaryChar
        ));

        emit(idx, "progress", com.novel.comic.util.Maps.of("message", "正在生成末张引流...", "done", 0, "total", 1));

        String raw = aiService.callAISmart(
                "你是专业的漫画末页引流策划师。",
                prompt,
                new AiService.StreamCallback() {
                    @Override public void onChunk(String text) {
                        emit(idx, "chunk", com.novel.comic.util.Maps.of("text", text));
                        sseService.appendStreamText(idx, text);
                    }
                    @Override public void onChunk(String text, int index) { onChunk(text); }
                    @Override public void onStatus(String message) { emit(idx, "status", com.novel.comic.util.Maps.of("message", message)); }
                    @Override public void onProgress(String message, int done, int total) {}
                },
                false, 2048, 0.7
        );

        Map<String, Object> lp = new LinkedHashMap<>();
        lp.put("characterName", primaryChar);
        lp.put("title", title);
        lp.put("prompt", raw);
        lastPagePrompt = lp;

        Map<String, Object> result = new LinkedHashMap<>(lp);
        result.put("type", "lastpage");
        steps.get(idx).result = result;
        emit(idx, "result", result);
        saveOutputFile("lastpage", lp);
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private void emit(int stepIndex, String event, Object data) {
        sseService.broadcast(stepIndex, event, data);
    }

    private void saveOutputFile(String name, Object data) {
        try {
            Path outputDir = config.getOutputPath();
            if (!Files.exists(outputDir)) Files.createDirectories(outputDir);
            String ts = Instant.now().toString().replace(":", "-").replace(".", "-");
            String fname = (novelTitle.isEmpty() ? "output" : novelTitle) + "_" + name + "_" + ts + ".json";
            Path fp = outputDir.resolve(fname);
            Files.write(fp, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Save failed: {}", e.getMessage());
        }
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