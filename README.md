# Agentic URL Shortener

## Overview

This project demonstrates an AI-native, governed software development lifecycle using a Java URL Shortener as the sample application. It separates deterministic orchestration and policy controls from the agent-execution adapter.

The solution includes Java 21, Spring Boot, REST APIs, JPA, persistent H2 storage, human release approval, policy enforcement, retries and rollback, audit history, reliability metrics, rate limiting, OpenAPI/Swagger, Actuator/Prometheus endpoints, and 26 automated tests: 20 unit and 6 integration tests.

## 1. Architecture

```text
Client / Postman / Swagger
          |
          v
Spring Boot REST API -> RateLimitFilter
          |
   +------+------------------+
   |                         |
   v                         v
UrlController          WorkflowController
   |                         |
   v                         v
UrlService              Orchestrator
   |                    /     |       \
   v                   v      v        v
ShortUrlRepository  PolicyGuard  StageExecutor  Audit/Persistence
                              |
                              v
                  DeterministicStageExecutor
                  (replaceable live-agent adapter)
```

Java owns API contracts, state, policies, approvals, persistence, recovery, audit records, and metrics. `StageExecutor` is an allow-listed boundary for a future credentialed AI/tool runtime; the supplied executor is deterministic and performs no network or privileged actions.

## 2. Project Structure

```text
src/main/java/com/schwab/assignment/
├── orchestration/   workflow graph, policies, audit, metrics, executor
├── url/             URL API, service, entity, repository
├── security/        rate limiting filter
└── config/          OpenAPI configuration

src/main/resources/
└── application.yml

src/test/java/com/schwab/assignment/
├── url/
├── orchestration/
├── security/
└── integration/
```

## 3. Workflow Model

```text
understand -> design -> implement ---\
                     \-> test -------+-> release approval -> release
understand -> docs -------------------/
```

The release stage requires successful implementation, testing, and documentation, then an explicit human approval. Stages move through `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, `ROLLED_BACK`, `BLOCKED`, or `AWAITING_APPROVAL` as appropriate.

## 4. Supported SDLC Scenarios

### Greenfield

```json
{"scenario":"greenfield","requirement":"Create URL shortening service"}
```

The workflow reaches `AWAITING_APPROVAL`; an approved release transitions it to `COMPLETED`.

### Brownfield

```json
{"scenario":"brownfield","requirement":"Add analytics to existing URL shortener"}
```

An implementation failure records `FAILED`, `ROLLED_BACK`, and `SAFE_STOPPED`. The operator may retry twice; further failure triggers `BLOCKED` and a manual-remediation fallback.

### Ambiguous requirement

Start with `Make links expire`, then clarify it to `Links expire after 30 days` through the re-plan API. The workflow creates plan version 2, invalidates approvals from version 1, re-executes the plan, and requires new release approval.

## 5. Policy Enforcement

`PolicyGuard` checks the requirement before each stage executes. It blocks:

- Security-control bypasses, such as `bypass security`, `disable audit`, or `skip validation`.
- Personal-data work without a retention requirement.
- Production/release work without a change-ticket reference.

A denial emits `POLICY_DENIED` and safe-stops the workflow. These rules are deterministic and not delegated to the agent executor.

## 6. Retry, Rollback, and Auditability

`MAX_RETRIES` is 2. A failure records rollback and safe-stop state. Exceeding the retry limit records `FALLBACK`, marks the failed stage `BLOCKED`, and requires manual remediation.

Audit records contain the workflow ID, timestamp, stage, action, detail, and plan version. Examples: `CREATED`, `POLICY_PASSED`, `APPROVED`, `FAILED`, `ROLLED_BACK`, `RETRY`, `RECOVERED`, `REPLANNED`, `FALLBACK`, and `COMPLETED`.

## 7. Persistence

The application uses a file-backed H2 database. JPA/Hibernate manages the local schema with `ddl-auto: update`; URL, workflow, and audit records survive application restarts while the H2 data files remain available.

## 8. Run and Test

```bash
mvn test
mvn spring-boot:run
```

`mvn test` runs the URL, controller, policy, workflow, and rate-limit unit tests. `mvn verify` additionally runs the persistent-H2, agent-runtime, and HTTP API integration tests.

Maven phases are separated: `mvn test` runs unit tests; `mvn verify` runs unit tests plus `*IntegrationTest` classes through Failsafe.

## Workflow behavior added for review

- Requirements that request expiry without a measurable duration (for example, `Make links expire`) stop in `AWAITING_CLARIFICATION`; a replan with `Expire links after 30 days` resumes normal planning.
- A `brownfield` workflow scans local Java sources and writes an impact-analysis artifact under `work/generated/`. It lists discovered controllers, services, entities, repositories, API-mapping sources, tests, and the observed URL data flow.
- Independent ready stages run as a bounded parallel batch through an `ExecutorService`; the orchestrator waits at each dependency gate before progressing. Release still requires successful implementation, test, documentation, and explicit human approval.
- Architecture decisions are documented in [docs/adr](docs/adr), including Java/Spring, H2, agent execution, policy controls, and parallel orchestration.

To demonstrate controlled real engineering validation from a trusted source checkout, enable command mode. It allow-lists only `mvn -q -DskipTests compile` for implementation and `mvn -q test` for validation; command exit codes become stage success or failure evidence.

```bash
mvn spring-boot:run -Dspring-boot.run.arguments='--agent.execution.mode=command'
```

## 9. Container Deployment

The repository includes a multi-stage `Dockerfile` and `compose.yaml`. Compose starts the application with a named Docker volume mounted at `/app/data`, where H2 stores its durable local database files.

```bash
docker compose up --build
```

The application is available at `http://localhost:8080`. Stop the stack with `docker compose down`; add `--volumes` only when intentionally deleting the local H2 data volume.

## 10. API and Operations

| Endpoint | Purpose |
| --- | --- |
| `POST /api/urls` | Create a short URL. |
| `GET /{code}` | Redirect and increment analytics. |
| `GET /api/urls/{code}/analytics` | Read aggregate redirects. |
| `POST /api/workflows` | Start a workflow. |
| `POST /api/workflows/{id}/approve` | Approve the current plan release. |
| `POST /api/workflows/{id}/failure` | Record failure, rollback, and safe stop. |
| `POST /api/workflows/{id}/retry` | Attempt bounded recovery. |
| `POST /api/workflows/{id}/replan` | Clarify a requirement and create a new plan. |
| `GET /api/metrics` | Domain workflow reliability metrics. |

OpenAPI: `http://localhost:8080/api-docs`  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
Health: `http://localhost:8080/actuator/health`  
Prometheus: `http://localhost:8080/actuator/prometheus`

Example URL creation:

```bash
curl -X POST http://localhost:8080/api/urls \
  -H 'content-type: application/json' \
  -d '{"destination":"https://example.com/products"}'
```

## 11. Limitations and Next Steps

This is a runnable assessment prototype. The release approver is caller-provided rather than authenticated, H2 is for local use, and the agent adapter is deterministic. A production increment should add identity/RBAC, a managed database with backup/retention, a shared gateway rate limiter, idempotency and URL reputation services, custom Prometheus workflow meters, and a credentialed live-agent adapter with strict tool allow-lists and output validation.
