package com.schwab.assignment.agents;

import com.schwab.assignment.graph.*;
import com.schwab.assignment.engine.*;

/** Provider-neutral boundary for deterministic, command-backed, or future live agents. */
public interface AgentAdapter {
  StageExecutor.ExecutionResult execute(Stage stage, Workflow workflow);
}
