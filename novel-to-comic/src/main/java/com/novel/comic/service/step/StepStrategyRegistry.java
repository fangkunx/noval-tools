package com.novel.comic.service.step;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that maps pipeline step ids to their corresponding
 * {@link StepStrategy} implementations.
 */
@Component
public class StepStrategyRegistry {

    private final Map<String, StepStrategy> strategies = new ConcurrentHashMap<>();

    public StepStrategyRegistry() {
        strategies.put("split",      new SplitStepStrategy());
        strategies.put("characters", new CharactersStepStrategy());
        strategies.put("turnaround", new TurnaroundStepStrategy());
        strategies.put("storyboard", new StoryboardStepStrategy());
        strategies.put("rename",     new RenameStepStrategy());
        strategies.put("prompts",    new PromptsStepStrategy());
        strategies.put("lastpage",   new LastPageStepStrategy());
    }

    /**
     * Resolve the strategy for the given step id.
     *
     * @param stepId the step id (e.g. "split", "characters", ...)
     * @return the matching strategy
     * @throws IllegalArgumentException if no strategy is registered for the id
     */
    public StepStrategy resolve(String stepId) {
        StepStrategy s = strategies.get(stepId);
        if (s == null) {
            throw new IllegalArgumentException("Unknown step: " + stepId);
        }
        return s;
    }
}