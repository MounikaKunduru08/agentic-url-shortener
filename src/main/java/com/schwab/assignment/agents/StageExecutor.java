package com.schwab.assignment.agents;

import com.schwab.assignment.graph.*;
import com.schwab.assignment.engine.*;

/** Adapter boundary for an allow-listed agent/tool runtime. Implementations must return validated results only. */
public interface StageExecutor extends AgentAdapter {
  @Override
  ExecutionResult execute(Stage stage, Workflow workflow);
  record ExecutionResult(boolean succeeded, String detail) {}
}
