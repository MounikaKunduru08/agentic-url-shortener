# ADR 007 Explicit Gates and Operator Interventions

## Decision

Represent scheduler gate outcomes as a sealed hierarchy—Pass, Fail, Block, and Escalate—and expose auditable pause, resume, and abort operations for workflow operators.

## Rationale

Governed orchestration must distinguish an ordinary success from a retryable failure, a branch-blocking policy violation, and a human escalation. A boolean cannot carry these different scheduling semantics safely. Operators also need a safe way to halt autonomous progress without modifying stored workflow data manually.

## Alternatives considered

Using exceptions for every outcome mixes expected governance decisions with application faults. A generic string status is easy to add but easy for a scheduler to ignore. Deleting or editing a run to stop it destroys audit lineage. A full distributed control center is deferred; REST intervention endpoints are the appropriate prototype boundary.

## Consequences

The scheduler handles every gate variant explicitly. Pause records the prior workflow status and resume restores it, including an approval wait. Abort is terminal and is recorded in the audit ledger. Production evolution would add authorization, lease/heartbeat ownership, and multi-node intervention delivery.
