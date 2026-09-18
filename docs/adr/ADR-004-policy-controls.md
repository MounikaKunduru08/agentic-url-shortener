# ADR 004 Policy Controls at Stage Gates

## Decision

Evaluate policy before every workflow stage, record the decision in the audit trail, and safe-stop a workflow when policy denies execution.

## Rationale

This keeps security, privacy, and release/change-ticket controls independent of individual agent implementations. A replacement live agent cannot bypass a gate by omitting a prompt instruction.

## Alternatives considered

Embedding policy text only in agent prompts is not enforceable or auditable. Putting all controls in controllers leaves internal stage transitions unprotected. A full external policy engine is deliberately out of scope for this prototype; `PolicyGuard` is the replaceable local boundary.

## Consequences

The current rules are intentionally small and deterministic. Production deployment would connect this boundary to approved policy-as-code, identity, change-management, and evidence systems.
