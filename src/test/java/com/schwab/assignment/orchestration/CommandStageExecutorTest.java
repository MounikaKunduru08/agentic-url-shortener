package com.schwab.assignment.orchestration;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandStageExecutorTest {
    @Test
    void testStageRunsMavenVerifyToIncludeIntegrationTests() {
        List<List<String>> commands = new ArrayList<>();
        CommandStageExecutor executor = new CommandStageExecutor(Path.of("."), Duration.ofSeconds(1), (command, workspace, timeout) -> {
            commands.add(command);
            return new CommandStageExecutor.CommandOutcome(true, 0, "all tests passed");
        });

        StageExecutor.ExecutionResult result = executor.execute(new Stage("test", Set.of(), false, "verification-agent"), workflow());

        assertTrue(result.succeeded());
        assertEquals(List.of("mvn", "-q", "verify"), commands.getFirst());
    }

    @Test
    void nonzeroCommandExitBecomesStageFailureEvidence() {
        CommandStageExecutor executor = new CommandStageExecutor(Path.of("."), Duration.ofSeconds(1), (command, workspace, timeout) -> new CommandStageExecutor.CommandOutcome(true, 1, "test failure"));

        StageExecutor.ExecutionResult result = executor.execute(new Stage("test", Set.of(), false, "verification-agent"), workflow());

        assertFalse(result.succeeded());
        assertTrue(result.detail().contains("exit=1"));
    }

    private Workflow workflow() {
        return new Workflow("greenfield", "Build URL shortener", Map.of());
    }
}
