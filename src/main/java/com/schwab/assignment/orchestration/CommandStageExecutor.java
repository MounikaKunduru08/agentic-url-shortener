package com.schwab.assignment.orchestration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Allow-listed local engineering commands. Enable only in a trusted checkout with AGENT_EXECUTION_MODE=command.
 */
@Component
@ConditionalOnProperty(name = "agent.execution.mode", havingValue = "command")
class CommandStageExecutor implements StageExecutor {
    public ExecutionResult execute(Stage stage, Workflow workflow) {
        if (!stage.id().equals("implement") && !stage.id().equals("test"))
            return new ExecutionResult(true, "no command required for " + stage.id());
        List<String> command = stage.id().equals("implement") ? List.of("mvn", "-q", "-DskipTests", "compile") : List.of("mvn", "-q", "test");
        try {
            Process process = new ProcessBuilder(command).directory(Path.of(System.getProperty("agent.workspace.root", ".")).toFile()).redirectErrorStream(true).start();
            boolean completed = process.waitFor(120, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!completed) {
                process.destroyForcibly();
                return new ExecutionResult(false, "command timed out after " + Duration.ofSeconds(120));
            }
            return new ExecutionResult(process.exitValue() == 0, "command=" + String.join(" ", command) + " exit=" + process.exitValue() + " output=" + output.substring(0, Math.min(output.length(), 500)));
        } catch (Exception e) {
            return new ExecutionResult(false, "command execution failed: " + e.getMessage());
        }
    }
}
