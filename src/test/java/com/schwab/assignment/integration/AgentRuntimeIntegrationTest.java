package com.schwab.assignment.integration;

import com.schwab.assignment.orchestration.Orchestrator;
import com.schwab.assignment.orchestration.Workflow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the active StageExecutor participates in the governed runtime through the workflow API. */
@SpringBootTest
class AgentRuntimeIntegrationTest {
  @Autowired Orchestrator orchestrator;
  @Test void stageExecutorProducesAuditableStageResults() {
    Workflow workflow=orchestrator.create("greenfield","Build URL shortener");
    assertTrue(workflow.audit().stream().anyMatch(event->event.detail().contains("requirements-agent")));
  }
}
