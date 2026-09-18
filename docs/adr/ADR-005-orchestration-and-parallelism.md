# ADR 005 Dependency-Graph Orchestration and Parallel Batches

## Decision

Model workflow stages as an explicit dependency graph. Dispatch all independent ready stages through an `ExecutorService`, wait for the batch to finish, and then evaluate the next dependency gate.

## Rationale

For example, `implement` and `test` are both ready after `design`, and `docs` may proceed after `understand`. Batch synchronization preserves the release gate: it cannot start until implementation, testing, and documentation have succeeded.

## Alternatives considered

A single sequential loop is easier but fails to demonstrate independent paths. Fully asynchronous mutation of a workflow object would increase throughput but makes audit ordering and transactional persistence substantially harder. This prototype uses parallel stage execution with synchronized state transitions as the safer middle ground.

## Consequences

The executor is process-local and bounded to three workers. A production orchestrator would use durable job queues, leases, idempotency keys, cancellation, and distributed tracing.
