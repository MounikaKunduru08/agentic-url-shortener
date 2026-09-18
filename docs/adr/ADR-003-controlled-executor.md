# ADR 003 Deterministic Versus Live Agent Execution

## Decision

Use a `StageExecutor` boundary with deterministic and command-backed implementations. Deterministic execution is the default; command execution is an explicit trusted-checkout mode.

## Alternatives

Direct LLM/tool calls from controllers would be simpler but would bypass governance. A live agent can later implement the same interface.

## Rationale

The command mode executes allow-listed Maven commands and records their result as workflow evidence. The deterministic mode keeps automated tests isolated and repeatable. Neither mode accepts arbitrary agent shell input; a future live LLM/tool agent must implement the same boundary and remain subject to policy and approval gates.
