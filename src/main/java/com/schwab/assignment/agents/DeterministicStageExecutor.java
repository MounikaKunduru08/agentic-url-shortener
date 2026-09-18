package com.schwab.assignment.agents;

import com.schwab.assignment.graph.*;
import com.schwab.assignment.engine.*;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/** Safe local adapter used by the prototype; it performs no network calls or privileged actions. */
@Component
@ConditionalOnProperty(name="agent.execution.mode", havingValue="deterministic", matchIfMissing=true)
class DeterministicStageExecutor implements StageExecutor {
  public ExecutionResult execute(Stage stage, Workflow workflow) {
    return new ExecutionResult(true, "validation gate passed by " + stage.agent());
  }
}
