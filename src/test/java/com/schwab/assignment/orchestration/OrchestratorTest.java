package com.schwab.assignment.orchestration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrchestratorTest {
  private final Orchestrator orchestrator = new Orchestrator();
  @Test void releaseRequiresHumanApprovalThenCompletes() {
    Workflow flow=orchestrator.create("greenfield","Create URL shortener");
    assertEquals(WorkflowStatus.AWAITING_APPROVAL,flow.status()); assertEquals(StageStatus.AWAITING_APPROVAL,flow.stages().get("release"));
    flow=orchestrator.approve(flow.id(),"engineering-manager");
    assertEquals(WorkflowStatus.COMPLETED,flow.status()); assertTrue(flow.audit().stream().anyMatch(e->e.action().equals("APPROVED")));
  }
  @Test void failureRecordsRollbackAndStopsSafely() {
    Workflow flow=orchestrator.create("brownfield","Add analytics"); flow=orchestrator.fail(flow.id(),"implement","security scan failure");
    assertEquals(WorkflowStatus.SAFE_STOPPED,flow.status()); assertEquals(1,flow.rollbacks()); assertEquals(StageStatus.ROLLED_BACK,flow.stages().get("implement"));
  }
  @Test void changedRequirementCreatesNewPlanVersion() {
    Workflow flow=orchestrator.create("ambiguous","Make links expire"); flow=orchestrator.replan(flow.id(),"Expiry defaults to 30 days");
    assertEquals(2,flow.planVersion()); assertTrue(flow.audit().stream().anyMatch(e->e.action().equals("REPLANNED")));
  }
  @Test void expiryWithoutDurationWaitsForClarification() {
    Workflow flow=orchestrator.create("ambiguous","Make links expire");
    assertEquals(WorkflowStatus.AWAITING_CLARIFICATION,flow.status());
    assertEquals(StageStatus.PENDING,flow.stages().get("understand"));
    assertTrue(flow.audit().stream().anyMatch(e->e.action().equals("AMBIGUITY_DETECTED")));
  }
  @Test void prohibitedSecurityBypassIsDeniedByPolicyAndSafeStopped() {
    Workflow flow=orchestrator.create("brownfield","Add analytics but bypass security validation");
    assertEquals(WorkflowStatus.SAFE_STOPPED,flow.status());
    assertTrue(flow.audit().stream().anyMatch(e->e.action().equals("POLICY_DENIED")));
  }
  @Test void retryIsBoundedAndEscalatesToManualFallback() {
    Workflow flow=orchestrator.create("brownfield","Add analytics");
    flow=orchestrator.fail(flow.id(),"implement","first validation failure"); flow=orchestrator.retry(flow.id());
    flow=orchestrator.fail(flow.id(),"implement","second validation failure"); flow=orchestrator.retry(flow.id());
    flow=orchestrator.fail(flow.id(),"implement","third validation failure"); flow=orchestrator.retry(flow.id());
    assertEquals(2,flow.retries()); assertTrue(flow.fallbackUsed()); assertEquals(WorkflowStatus.SAFE_STOPPED,flow.status());
    assertTrue(flow.audit().stream().anyMatch(e->e.action().equals("FALLBACK")));
  }
}
