package com.schwab.assignment.engine;

import com.schwab.assignment.agents.StageExecutor;
import com.schwab.assignment.artifacts.EngineeringArtifacts;
import com.schwab.assignment.graph.StageStatus;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ParallelOrchestrationTest {
  @Test void independentImplementAndTestStagesOverlapBeforeReleaseGate() {
    CountDownLatch independentStagesStarted=new CountDownLatch(2);
    Set<String> observed=ConcurrentHashMap.newKeySet();
    StageExecutor executor=(stage,workflow)->{
      if(stage.id().equals("implement")||stage.id().equals("test")) {
        observed.add(stage.id()); independentStagesStarted.countDown();
        try {
          if(!independentStagesStarted.await(1,TimeUnit.SECONDS)) return new StageExecutor.ExecutionResult(false,"stages did not overlap");
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt(); return new StageExecutor.ExecutionResult(false,"parallelism check interrupted");
        }
      }
      return new StageExecutor.ExecutionResult(true,"completed by "+stage.agent());
    };

    Workflow workflow=new Orchestrator(executor,EngineeringArtifacts.disabled()).create("greenfield","Build URL shortener");

    assertEquals(Set.of("implement","test"),observed);
    assertEquals(WorkflowStatus.AWAITING_APPROVAL,workflow.status());
    assertEquals(StageStatus.AWAITING_APPROVAL,workflow.stages().get("release"));
  }
}
