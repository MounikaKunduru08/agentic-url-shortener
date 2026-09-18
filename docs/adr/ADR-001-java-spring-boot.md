# ADR 001 Java and Spring Boot

## Decision

Use Java 21 and Spring Boot for the service and orchestration API.

## Alternatives

Python/FastAPI would be faster for a script-first prototype; Node.js would be suitable for an event-oriented API.

## Rationale

Spring Boot provides mature REST, validation, JPA, observability, and testing support in one maintainable Java service.
