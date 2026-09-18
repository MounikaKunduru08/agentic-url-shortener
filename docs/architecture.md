# Architecture

## Intent

The application uses a small ports-and-adapters design so workflow behavior is not coupled to Spring MVC, H2, or a particular agent provider. It is intentionally sophisticated enough to show governed-agent design, while remaining understandable for an assessment prototype.

## Dependency direction

```text
api, url, security (inbound adapters)
                |
                v
            engine --------> graph
             |  |              |
             |  +------------> policy (PolicyEvaluator port)
             |
             +--------------> agents (StageExecutor port)
             |
             +--------------> WorkflowStore port
                                    ^
                                    |
                         persistence (H2/JPA adapter)

ledger and report are read-model concerns; artifacts produces reviewer evidence.
```

## Module responsibilities

| Module | Responsibility | May be replaced by |
| --- | --- | --- |
| `graph` | Stage definitions, validated dependencies, execution layers, gate outcomes | A database-backed or externally authored workflow definition |
| `engine` | Workflow aggregate, scheduler, immutable snapshots, persistence port | No framework-specific replacement required |
| `policy` | `PolicyEvaluator` port and local policy rules | OPA, Cedar, or a central policy service |
| `agents` | `StageExecutor` port and deterministic/command adapters | Credentialed LLM/tool adapter with allow-listed tools |
| `persistence` | `JpaWorkflowStore`, JPA records, H2-specific storage | PostgreSQL/JPA, event store, or managed workflow store |
| `ledger` | Domain audit-event representation | Dedicated immutable audit stream |
| `report` | Projection of workflow, audit, and reliability data | BI/dashboard read model |
| `api` | HTTP-only request/response adapters | Messaging, CLI, or gRPC adapter |
| `artifacts` | Requirement/task/brownfield evidence artifacts | SCM-aware impact-analysis provider |

## Workflow lifecycle

1. The API calls `Orchestrator`, the application-facing engine facade.
2. `WorkflowDefinition` supplies a validated DAG.
3. `WorkflowScheduler` admits dependency-ready stages in bounded parallel batches.
4. Every stage passes `PolicyEvaluator` before its `StageExecutor` runs.
5. The engine writes an immutable `WorkflowSnapshot` through `WorkflowStore` and persists append-only audit events.
6. A human approval gate blocks release. Failure supports rollback, bounded retries, fallback, safe stop, pause, resume, and abort.

## Extension rules

- Add a new persistence technology by implementing `WorkflowStore`; do not add repository code to `engine`.
- Add a new agent provider by implementing `StageExecutor`; validate all tool outputs before returning an `ExecutionResult`.
- Add a new policy provider by implementing `PolicyEvaluator`; do not embed policy checks in controllers or executors.
- Keep controllers transport-focused and keep JPA entities inside `persistence`.
