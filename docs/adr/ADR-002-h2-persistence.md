# ADR 002 File Backed H2 Persistence

## Decision

Use file-backed H2 for the self-contained prototype.

## Alternatives

Managed PostgreSQL with versioned migrations provides stronger production durability and operations.

## Rationale

H2 keeps setup and Docker deployment simple while preserving URL, workflow, and audit state across application restarts through a mounted volume.
