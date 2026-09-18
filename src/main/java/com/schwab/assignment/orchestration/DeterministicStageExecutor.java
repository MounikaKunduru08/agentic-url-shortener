package com.schwab.assignment.orchestration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Safe local adapter used by the prototype; it performs no network calls or privileged actions.
 */
@Component
@ConditionalOnProperty(name = "agent.execution.mode", havingValue = "deterministic", matchIfMissing = true)
class DeterministicStageExecutor implements StageExecutor {
    public ExecutionResult execute(Stage stage, Workflow workflow) {
        return new ExecutionResult(true, "validation gate passed by " + stage.agent());
    }
}
