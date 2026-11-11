package com.novel.comic.service.step;

/**
 * Strategy for executing a single pipeline step.
 * Each implementation encapsulates the business logic of one step type
 * (split, characters, turnaround, storyboard, rename, prompts, lastpage).
 */
@FunctionalInterface
public interface StepStrategy {

    /**
     * Execute this pipeline step.
     *
     * @param ctx the execution context providing access to shared state and services
     * @throws Exception if the step fails
     */
    void execute(StepContext ctx) throws Exception;
}