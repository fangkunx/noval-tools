package com.novel.comic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SSE (Server-Sent Events) connections per step.
 * Equivalent to the Node.js EventBus pattern.
 */
@Service
public class SseService {
    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    // stepIndex -> list of active emitters
    private final Map<Integer, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // stepIndex -> accumulated stream text (for late joiners)
    private final Map<Integer, StringBuilder> streamTexts = new ConcurrentHashMap<>();

    /**
     * Create and register a new SSE emitter for a step.
     */
    public SseEmitter createEmitter(int stepIndex) {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 min timeout
        emitters.computeIfAbsent(stepIndex, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(stepIndex, emitter));
        emitter.onTimeout(() -> remove(stepIndex, emitter));
        emitter.onError(e -> remove(stepIndex, emitter));

        return emitter;
    }

    private void remove(int stepIndex, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(stepIndex);
        if (list != null) list.remove(emitter);
    }

    /**
     * Broadcast an event to all emitters for a step.
     */
    public void broadcast(int stepIndex, String event, Object data) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(stepIndex);
        if (list == null || list.isEmpty()) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (IOException e) {
                list.remove(emitter);
            }
        }
    }

    /**
     * Accumulate stream text for a step (for late-joining SSE clients).
     */
    public void appendStreamText(int stepIndex, String text) {
        streamTexts.computeIfAbsent(stepIndex, k -> new StringBuilder()).append(text);
    }

    /**
     * Get accumulated stream text for a step.
     */
    public String getStreamText(int stepIndex) {
        StringBuilder sb = streamTexts.get(stepIndex);
        return sb != null ? sb.toString() : "";
    }

    /**
     * Clear stream text for a step.
     */
    public void clearStreamText(int stepIndex) {
        streamTexts.remove(stepIndex);
    }

    /**
     * Remove all emitters for all steps.
     */
    public void clearAll() {
        for (CopyOnWriteArrayList<SseEmitter> list : emitters.values()) {
            for (SseEmitter e : list) {
                try { e.complete(); } catch (Exception ignored) {}
            }
            list.clear();
        }
        emitters.clear();
        streamTexts.clear();
    }

    /**
     * Check if any emitters exist for a step.
     */
    public boolean hasEmitters(int stepIndex) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(stepIndex);
        return list != null && !list.isEmpty();
    }
}