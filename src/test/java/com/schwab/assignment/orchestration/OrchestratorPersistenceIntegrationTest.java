package com.schwab.assignment.orchestration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OrchestratorPersistenceIntegrationTest {
  @Autowired Orchestrator orchestrator;
  @Test void restoresPersistedWorkflowAndAuditTrail() {
    Workflow created=orchestrator.create("greenfield","Build URL shortener");
    Workflow restored=orchestrator.get(created.id());
    assertEquals(created.id(),restored.id()); assertEquals(WorkflowStatus.AWAITING_APPROVAL,restored.status());
    assertTrue(restored.audit().stream().anyMatch(event->event.action().equals("CREATED")));
  }
}
