package com.schwab.assignment.orchestration;

/**
 * Adapter boundary for an allow-listed agent/tool runtime. Implementations must return validated results only.
 */
public interface StageExecutor {
    ExecutionResult execute(Stage stage, Workflow workflow);

    record ExecutionResult(boolean succeeded, String detail) {
    }
}
