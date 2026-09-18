package com.schwab.assignment.engine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrchestratorPersistenceIntegrationTest {
  @Autowired Orchestrator orchestrator;
  @Test void restoresPersistedWorkflowAndAuditTrail() {
    Workflow created=orchestrator.create("greenfield","Build URL shortener");
    Workflow restored=orchestrator.get(created.id());
    assertEquals(created.id(),restored.id()); assertEquals(WorkflowStatus.AWAITING_APPROVAL,restored.status());
    assertEquals(created.audit(),restored.audit());
    assertTrue(restored.audit().stream().anyMatch(event->event.action().equals("CREATED")));
  }
}
