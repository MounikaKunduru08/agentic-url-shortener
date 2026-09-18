package com.schwab.assignment.engine;

import com.schwab.assignment.graph.Stage;
import com.schwab.assignment.graph.StageStatus;
import com.schwab.assignment.graph.WorkflowGraph;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowTest {
  private static final Map<String,Stage> GRAPH=Map.of("build",new Stage("build",Set.of(),false,"test-agent"));
  @Test void failureProducesRollbackAndSafeStop() {
    Workflow workflow=new Workflow("brownfield","Change analytics",GRAPH); workflow.fail("build","validation failed");
    assertEquals(WorkflowStatus.SAFE_STOPPED,workflow.status()); assertEquals(StageStatus.ROLLED_BACK,workflow.stages().get("build"));
  }
  @Test void retryRequiresARecordedFailure() {
    Workflow workflow=new Workflow("greenfield","Build",GRAPH);
    assertThrows(IllegalStateException.class,workflow::retry);
  }
  @Test void selectiveReplanInvalidatesOnlyTheChangedStageAndItsDownstreamStages() {
    Map<String,Stage> graph=WorkflowGraph.of(
        new Stage("understand",Set.of(),false,"agent"),new Stage("design",Set.of("understand"),false,"agent"),
        new Stage("implement",Set.of("design"),false,"agent"),new Stage("docs",Set.of("understand"),false,"agent"),
        new Stage("release",Set.of("implement","docs"),true,"agent"));
    Workflow workflow=new Workflow("brownfield","Add analytics",graph);
    graph.keySet().forEach(stage->workflow.setStage(stage,StageStatus.SUCCEEDED,"prior plan"));

    workflow.replan("Correct the documentation","docs");

    assertEquals(StageStatus.SUCCEEDED,workflow.stages().get("implement"));
    assertEquals(StageStatus.PENDING,workflow.stages().get("docs"));
    assertEquals(StageStatus.PENDING,workflow.stages().get("release"));
    assertTrue(workflow.audit().getLast().detail().contains("invalidated=docs,release"));
  }
  @Test void pauseResumeAndAbortAreAuditableOperatorInterventions() {
    Workflow workflow=new Workflow("greenfield","Build",GRAPH);
    workflow.pause("operator"); assertEquals(WorkflowStatus.PAUSED,workflow.status());
    workflow.resume("operator"); assertEquals(WorkflowStatus.RUNNING,workflow.status());
    workflow.abort("operator","manual stop"); assertEquals(WorkflowStatus.ABORTED,workflow.status());
    assertTrue(workflow.audit().stream().anyMatch(event->event.action().equals("ABORTED")));
  }
}
