# Architecture and Engineering Decisions

## Purpose

Describe the architecture, execution flow, governance model, validation approach, and known boundaries of the Agentic Software Engineering System — URL Shortener prototype.

## Components

- **URL API (`url`)**: REST endpoints, URL validation, persistence, redirects, click tracking and expiration.
- **Requirement planner (`agent`)**: classifies a requirement as greenfield, brownfield or ambiguous and produces a task plan.
- **Workflow orchestrator (`orchestration`)**: owns workflow state, dependency scheduling, task status transitions, retries, and downstream failure handling.
- **Policy service (`policy`)**: evaluates whether a task is allowed to execute.
- **Approval service (`approval`)**: handles human decisions at approval gates.
- **Audit service (`audit`)**: records workflow/task events with timestamps and details.
- **Analytics/observability (`analytics`)**: exposes workflow counts and reliability metrics.
- **Global exception handling (`common`)**: provides consistent API error handling.

## Execution flow

1. Receive a requirement through `POST /api/workflows`.
2. Classify the scenario.
3. If ambiguous, move the workflow to `NEEDS_CLARIFICATION`.
4. Otherwise, build and validate a dependency graph.
5. Schedule tasks whose dependencies are complete.
6. Evaluate policy immediately before execution.
7. Record task start/attempt/success/failure events.
8. Retry a failed task within the configured attempt limit.
9. Block dependent tasks if a prerequisite fails or is rejected.
10. Pause at human approval gates.
11. On approval, resume the workflow and finalize its status.
12. Expose the resulting workflow, audit events and metrics.

## Dependency graph example

```text
                 T1: Requirement analysis
                    /       |       \
                   v        v        v
              T2: Design  T3: API  T4: Test plan
                   \        |        /
                    \       |       /
                     v      v      v
                  T5: Release readiness
                         |
                    Human approval
                         |
                    Final workflow
```

The actual graph depends on the selected scenario. Independent tasks can run concurrently when their dependencies are satisfied.

## Governance and safety

- Tasks must not execute before their dependencies complete.
- Policy evaluation occurs before task execution.
- Retries are bounded to prevent unending loops.
- Failed or rejected prerequisites prevent downstream execution.
- High-impact release-readiness work pauses for human approval.
- Audit events provide a trace of the workflow lifecycle.
- Ambiguous requirements should pause rather than silently assume acceptance criteria.

## Engineering decisions and trade-offs

### Deterministic simulated executor
The executor returns a simulated completion message. This makes workflow mechanics repeatable and unit-testable. It does not prove that code was generated, tests were actually run by an agent, or a deployment was performed.

### In-memory workflow state
This simplifies the prototype but loses workflow, approval and audit state on restart. Production requires durable storage and recovery semantics.

### H2
H2 is convenient for local development. Production requires a durable database, schema migrations, operational backups and suitable indexes.

### Human approval
Approval is an explicit control point. The prototype does not provide production authentication/authorization; approval endpoints must be secured before deployment.

## Validation evidence

The user reports that the full test suite passes. The test classes visible in the project include:
- `RequirementPlannerTest`
- `PolicyServiceTest`
- `WorkflowGovernanceTest`
- `WorkflowOrchestratorTest`
- `ShortUrlIntegrationTest`
- `AgenticUrlShortenerApplicationTests`

Validation should also be performed through Postman for URL creation/redirect/click tracking, approval completion, ambiguous requirement handling, audit history and metrics.

## Limitations

- No runtime LLM integration.
- Task execution is simulated.
- Durable persistence for workflows and audit may be absent.
- Production security, rate limiting and abuse prevention are outside scope.
- Rollback, dynamic replanning and MTTR should only be claimed where implementation and tests provide evidence.
- Clarification state should be tested end-to-end; a status alone does not prove that a question is captured or that the workflow resumes after clarification.

## Future architecture

A production-oriented extension could add a constrained LLM planner, tool-based coding/test/documentation agents, durable workflow state, authenticated approval, explicit rollback plans, replanning with decision lineage, and production metrics with documented definitions.
