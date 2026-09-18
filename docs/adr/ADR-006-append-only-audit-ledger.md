# ADR 006 Append-Only Audit Ledger and Derived Run Reports

## Decision

Persist workflow state as a replaceable snapshot, but append each new audit event only once. Generate reviewer-facing run reports from that durable event lineage and current workflow state.

## Rationale

Execution history should explain how a workflow reached its current state. Replacing a mutable snapshot is appropriate for the current stage map; deleting and recreating audit history is not. An append-only event sequence supports reliable review, reporting, and future export to a dedicated event store.

## Alternatives considered

Keeping only a current workflow row loses decision lineage. Rewriting every audit row on every state transition makes an audit trail mutable in practice. A full event-sourced system was not introduced because it would require projections, replay tooling, and migration work beyond this assessment prototype.

## Consequences

The current H2 ledger deduplicates events by their immutable event value before insertion. The `GET /api/workflows/{id}/report` endpoint derives timeline and action-count views from persisted workflow/audit data. A production implementation would use database uniqueness constraints, immutable event IDs, retention controls, and a centralized event store.
