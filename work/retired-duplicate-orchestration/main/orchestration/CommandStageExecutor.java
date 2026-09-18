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
    private final Path workspace;
    private final Duration timeout;
    private final CommandRunner runner;

    CommandStageExecutor() {
        this(Path.of(System.getProperty("agent.workspace.root", ".")), Duration.ofSeconds(120), new ProcessCommandRunner());
    }

    CommandStageExecutor(Path workspace, Duration timeout, CommandRunner runner) {
        this.workspace = workspace;
        this.timeout = timeout;
        this.runner = runner;
    }

    public ExecutionResult execute(Stage stage, Workflow workflow) {
        if (!stage.id().equals("implement") && !stage.id().equals("test"))
            return new ExecutionResult(true, "no command required for " + stage.id());
        List<String> command = stage.id().equals("implement") ? List.of("mvn", "-q", "-DskipTests", "compile") : List.of("mvn", "-q", "verify");
        try {
            CommandOutcome outcome = runner.run(command, workspace, timeout);
            if (!outcome.completed()) return new ExecutionResult(false, "command timed out after " + timeout);
            return new ExecutionResult(outcome.exitCode() == 0, "command=" + String.join(" ", command) + " exit=" + outcome.exitCode() + " output=" + outcome.output().substring(0, Math.min(outcome.output().length(), 500)));
        } catch (Exception e) {
            return new ExecutionResult(false, "command execution failed: " + e.getMessage());
        }
    }

    @FunctionalInterface
    interface CommandRunner {
        CommandOutcome run(List<String> command, Path workspace, Duration timeout) throws Exception;
    }

    record CommandOutcome(boolean completed, int exitCode, String output) {
    }

    private static final class ProcessCommandRunner implements CommandRunner {
        public CommandOutcome run(List<String> command, Path workspace, Duration timeout) throws Exception {
            Process process = new ProcessBuilder(command).directory(workspace.toFile()).redirectErrorStream(true).start();
            boolean completed = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!completed) process.destroyForcibly();
            return new CommandOutcome(completed, completed ? process.exitValue() : -1, output);
        }
    }
}
