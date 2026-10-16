# Agentic Software Engineering System — URL Shortener

## 1. Project Overview

This project is a Java/Spring Boot prototype containing two parts:

1. **URL shortener REST API** — creates short links, redirects to the original URL, tracks clicks, validates destinations, and supports optional expiration.
2. **Workflow orchestration prototype** — accepts an engineering requirement, classifies the scenario, creates a dependency-based task plan, executes tasks, applies policy checks, records audit events, exposes metrics, and pauses for human approval or clarification when required.

The purpose is to demonstrate controlled workflow orchestration and engineering governance. **The current task executor is simulated. It does not use an LLM to generate or modify source code.** An LLM integration is not explicitly required by the assignment, but real code-generation and validation agents would be a future extension.

## 2. Technology Stack

- Java 17 language target
- Spring Boot
- Spring MVC
- Spring Data JPA
- H2 database
- Maven
- JUnit 5 and Spring Boot Test

Use a JDK compatible with the Spring Boot version configured in `pom.xml`. The project language target is Java 17; the application may also run on a newer compatible JDK.

## 3. Architecture

### Main packages

| Package | Responsibility |
|---|---|
| `url` | URL creation, destination validation, persistence, redirects, click tracking and expiration |
| `agent` | Requirement scenario classification and plan creation |
| `orchestration` | Workflow runs, task dependency graph, execution, retries and state transitions |
| `policy` | Policy decisions before task execution |
| `approval` | Recording and handling human approval decisions |
| `audit` | Workflow/task event records |
| `analytics` | Workflow metrics and observability endpoints |
| `common` | Shared exception handling |

### High-level workflow

```text
Requirement
    |
    v
Scenario classification
    |
    +---- Ambiguous ----> NEEDS_CLARIFICATION
    |
    v
Create dependency graph
    |
    v
Policy check -> Task execution -> Audit event
    |
    +---- Retry within configured limit on failure
    |
    +---- Failed/rejected dependency -> downstream BLOCKED
    |
    v
Human approval checkpoint
    |
    +---- Rejected -> workflow does not proceed to release
    |
    +---- Approved -> continue and finalize
    |
    v
Workflow result + audit trail + metrics
```

The orchestration layer supports task dependencies and eligible independent tasks. Task execution in this prototype returns simulated completion output, so a `COMPLETED` task means the simulated executor completed—not that source code was generated or a real test suite was run by an agent.

## 4. URL Shortener API

Base URL: `http://localhost:8080`

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/urls` | Create a short URL |
| `GET` | `/api/urls/{code}` | Retrieve URL details and click count |
| `GET` | `/r/{code}` | Redirect to the original URL and track a click |

### Create a short URL

Request:

```http
POST /api/urls
Content-Type: application/json
```

```json
{
  "originalUrl": "https://example.com/products"
}
```

Expected result: `201 Created`, with the generated short code and short URL in the response.

Optional expiration can be supplied as a future date-time supported by the request model:

```json
{
  "originalUrl": "https://example.com/products",
  "expiresAt": "2030-01-01T00:00:00"
}
```

The exact response fields are defined by the current controller implementation.

### Redirect

Call `GET /r/{code}`. A valid, non-expired code should return a redirect to the original URL. A redirect increments the click count.

### Validation checks

- Invalid or unsupported schemes such as `javascript:` should be rejected.
- Unknown short codes should return not found.
- Expired links should not redirect.
- Expiration on creation must be in the future.

## 5. Workflow API

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/workflows` | Start a workflow |
| `GET` | `/api/workflows/{runId}` | Retrieve workflow state |
| `GET` | `/api/workflows/{runId}/summary` | Retrieve a workflow summary |
| `POST` | `/api/workflows/{runId}/tasks/{taskId}/approval` | Approve or reject a task awaiting approval |

Additional audit and metrics endpoints are exposed by the observability controller. Use the mappings in `WorkflowObservabilityController` as the source of truth for their exact paths.

### Start a greenfield workflow

```http
POST /api/workflows
Content-Type: application/json
```

```json
{
  "requirement": "Build a new URL shortener service"
}
```

The response includes a workflow `runId`, scenario-derived plan, task statuses and workflow status. Execution is asynchronous; poll `GET /api/workflows/{runId}` to inspect the latest state.

### Human approval

When a task is in `WAITING_FOR_APPROVAL`, send:

```http
POST /api/workflows/{runId}/tasks/T5/approval
Content-Type: application/json
```

```json
{
  "approved": true
}
```

Use `false` to reject. Approval decisions should only be submitted for tasks actually waiting for approval.

### Ambiguous requirement

Example:

```json
{
  "requirement": "Make the URL shortener faster and better."
}
```

The workflow should enter `NEEDS_CLARIFICATION` rather than treating the vague requirement as an approved implementation plan. The current implementation's clarification-question content and resume flow should be described as supported only to the extent verified by the current code and tests.

## 6. Scenario Coverage

### Greenfield
Example: `Build a new URL shortener service`

The planner classifies the request as a new-system scenario and creates tasks for requirements, design, API contracts, test planning and release readiness.

### Brownfield
Example: `Enhance the existing URL shortener to support custom aliases, preserve existing redirect behavior, and add regression tests.`

The planner classifies the request as an existing-system change and creates tasks for codebase inspection, impact analysis, implementation/test planning, regression validation and release approval.

### Ambiguous
Example: `Make the URL shortener faster and better.`

The planner classifies the request as ambiguous and pauses the workflow for clarification instead of continuing as if measurable acceptance criteria were available.

**Important:** scenario classification and simulated task execution are not equivalent to an autonomous coding agent. The prototype does not currently have an LLM write code, run arbitrary engineering tools, or automatically commit changes.

## 7. Reliability, Governance and Observability

The prototype demonstrates these mechanisms:

- Explicit task dependencies and dependency validation.
- Detection of invalid dependency graphs/cycles, where implemented by the orchestrator.
- Bounded task retries.
- Downstream blocking after failed dependencies.
- Policy evaluation before task execution.
- Human approval checkpoints.
- Audit events for workflow and task lifecycle activity.
- Workflow metrics such as totals, success rate, task attempts, retries and latency.

Metric interpretation:
- **Success rate** depends on the workflows currently counted by the in-memory analytics service.
- **Retry count** reflects retry attempts tracked by the implementation.
- **Average latency** reflects the timestamps and aggregation logic implemented by the metrics service. Confirm whether waiting-for-human-approval time is included before interpreting it as execution latency.
- **MTTR, rollback and dynamic replanning** are assignment goals; do not claim these are fully implemented unless the code and tests demonstrate them.

## 8. Setup and Run

1. Install a compatible JDK and Maven.
2. Open the project in IntelliJ IDEA.
3. Reload/sync the Maven project.
4. Confirm `application.properties` configures the H2 datasource.
5. Run `AgenticUrlShortenerApplication`.
6. Use Postman against `http://localhost:8080`.

Run the full test suite from the project root:

```bash
mvn clean test
```

The project uses an H2 in-memory database if configured as `jdbc:h2:mem:urldb`; stored URL data resets when the application restarts. Workflow runs and audit/approval data may also be in-memory.

If the H2 console is enabled, it is available at `http://localhost:8080/h2-console`. Use the JDBC URL configured in `application.properties`.

## 9. Test Plan

The current test suite includes tests for:
- URL creation and validation.
- Redirect behavior and click tracking.
- Unknown and expired URLs.
- Requirement scenario classification.
- Policy decisions.
- Workflow retries and dependency blocking.
- Governance, approval and clarification behavior.
- Application context startup.

Run `mvn clean test` and verify the final summary reports no failures. In Postman, also verify:
1. Create a URL.
2. Redirect using its code and check click count.
3. Submit an invalid destination.
4. Start a greenfield workflow and poll its state.
5. Approve the waiting release-readiness task and verify the final status.
6. Submit a vague requirement and verify `NEEDS_CLARIFICATION`.
7. Inspect the audit events and metrics endpoints.

## 10. Security, Trade-offs and Limitations

- Workflow plans and task outputs are simulated; no LLM is used at runtime.
- Workflow state, approvals and audit history are not durable if held in memory.
- Approval endpoints are not protected by authentication/authorization in this prototype.
- There is no production-grade secret management, distributed locking, durable queue or multi-instance coordination.
- URL shortener abuse controls (rate limiting, domain reputation checks, malware/phishing screening, quotas and administrative takedown) are outside the current scope.
- Rollback and dynamic replanning should not be described as production-ready unless they are implemented and tested.
- The URL API uses a prototype H2 configuration; production would require a durable database, migrations, indexes, monitoring and backup/restore practices.

## 11. Future Work

- Integrate a constrained LLM planner for requirement interpretation and task proposal.
- Add actual tool-backed execution agents for code changes, test execution and documentation generation.
- Require human approval before high-impact file, deployment or release operations.
- Persist workflow state, approval decisions and audit events.
- Implement clarification submission and safe workflow resumption.
- Add explicit rollback plans and dynamic replanning with decision lineage.
- Add authentication/authorization, rate limits and policy configuration.
- Add integration tests for audit/metrics endpoints and concurrency/deadlock scenarios.
- Add production observability, MTTR and end-to-end latency definitions.

## 12. Final Engineering Summary

This is a runnable prototype that combines a URL-shortening API with a governed workflow orchestration layer. Its strongest demonstrated capabilities are dependency-aware task planning, bounded retries, failure propagation, policy checks, audit events, human approval, scenario classification and workflow metrics.

The key trade-off is that the orchestration lifecycle is demonstrated with a simulated executor rather than a real LLM-powered coding agent. This keeps the prototype deterministic and testable while leaving code-generation agents, durable state, security hardening, rollback and production deployment as clearly identified extensions.
