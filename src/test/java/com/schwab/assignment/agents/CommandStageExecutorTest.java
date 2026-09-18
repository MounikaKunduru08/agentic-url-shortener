package com.schwab.assignment.agents;

import com.schwab.assignment.engine.Workflow;
import com.schwab.assignment.graph.Stage;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CommandStageExecutorTest {
  @Test void testStageRunsMavenVerifyToIncludeIntegrationTests() {
    List<List<String>> commands=new ArrayList<>();
    CommandStageExecutor executor=new CommandStageExecutor(Path.of("."),Duration.ofSeconds(1),(command,workspace,timeout)->{
      commands.add(command); return new CommandStageExecutor.CommandOutcome(true,0,"all tests passed");
    });

    StageExecutor.ExecutionResult result=executor.execute(new Stage("test",Set.of(),false,"verification-agent"),workflow());

    assertTrue(result.succeeded());
    assertEquals(List.of("mvn","-q","verify"),commands.getFirst());
  }

  @Test void nonzeroCommandExitBecomesStageFailureEvidence() {
    CommandStageExecutor executor=new CommandStageExecutor(Path.of("."),Duration.ofSeconds(1),(command,workspace,timeout)->new CommandStageExecutor.CommandOutcome(true,1,"test failure"));

    StageExecutor.ExecutionResult result=executor.execute(new Stage("test",Set.of(),false,"verification-agent"),workflow());

    assertFalse(result.succeeded()); assertTrue(result.detail().contains("exit=1"));
  }

  @Test void disabledShellExecutionShortCircuitsBeforeProcessRunner() {
    boolean[] runnerCalled={false};
    CommandStageExecutor executor=new CommandStageExecutor(Path.of("."),Duration.ofSeconds(1),(command,workspace,timeout)->{
      runnerCalled[0]=true; return new CommandStageExecutor.CommandOutcome(true,0,"should not run");
    },true);

    StageExecutor.ExecutionResult result=executor.execute(new Stage("test",Set.of(),false,"verification-agent"),workflow());

    assertTrue(result.succeeded()); assertFalse(runnerCalled[0]); assertTrue(result.detail().contains("shell execution disabled"));
  }

  private Workflow workflow(){return new Workflow("greenfield","Build URL shortener",Map.of());}
}
