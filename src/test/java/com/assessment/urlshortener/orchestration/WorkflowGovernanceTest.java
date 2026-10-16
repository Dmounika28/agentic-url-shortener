package com.assessment.urlshortener.orchestration;

import com.assessment.urlshortener.agent.RequirementPlanner;
import com.assessment.urlshortener.approval.ApprovalService;
import com.assessment.urlshortener.audit.AuditService;
import com.assessment.urlshortener.policy.PolicyService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowGovernanceTest {

    @Test
    void shouldPauseAndResumeAfterHumanApproval() throws Exception {
        AuditService auditService = new AuditService();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        task -> "Completed: " + task.getDescription(),
                        new RequirementPlanner(),
                        new PolicyService(),
                        auditService,
                        new ApprovalService(auditService)
                );

        try {
            WorkflowRun run = orchestrator.start(
                    "Build a new URL shortener service"
            );

            waitUntil(() ->
                    run.getStatus()
                            == WorkflowStatus.WAITING_FOR_APPROVAL);

            EngineeringTask approvalTask = run.getTasks().stream()
                    .filter(t -> t.getStatus()
                            == TaskStatus.WAITING_FOR_APPROVAL)
                    .findFirst()
                    .orElseThrow();

            assertEquals("T5", approvalTask.getId());

            orchestrator.decideApproval(
                    run.getRunId(),
                    approvalTask.getId(),
                    true
            );

            waitUntil(() ->
                    run.getStatus() == WorkflowStatus.COMPLETED);

            assertEquals(
                    TaskStatus.COMPLETED,
                    approvalTask.getStatus()
            );
            assertEquals(Boolean.TRUE, approvalTask.getApproved());

            assertTrue(auditService.getEvents(run.getRunId())
                    .stream()
                    .anyMatch(e ->
                            e.eventType().equals("APPROVAL_GRANTED")));

        } finally {
            orchestrator.shutdown();
        }
    }

    @Test
    void shouldRequestClarificationForAmbiguousRequirement() {
        AuditService auditService = new AuditService();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        task -> "Completed",
                        new RequirementPlanner(),
                        new PolicyService(),
                        auditService,
                        new ApprovalService(auditService)
                );

        try {
            WorkflowRun run = orchestrator.start(
                    "Something may be wrong with our existing URL service"
            );

            assertEquals(
                    WorkflowStatus.NEEDS_CLARIFICATION,
                    run.getStatus()
            );

            assertTrue(auditService.getEvents(run.getRunId())
                    .stream()
                    .anyMatch(e ->
                            e.eventType().equals(
                                    "CLARIFICATION_REQUIRED")));

        } finally {
            orchestrator.shutdown();
        }
    }

    @Test
    void shouldResumeAfterClarification() throws Exception {
        AuditService auditService = new AuditService();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        task -> "Completed: " + task.getDescription(),
                        new RequirementPlanner(),
                        new PolicyService(),
                        auditService,
                        new ApprovalService(auditService)
                );

        try {
            WorkflowRun run = orchestrator.start(
                    "Something may be wrong with our existing URL service"
            );

            assertEquals(
                    WorkflowStatus.NEEDS_CLARIFICATION,
                    run.getStatus()
            );

            orchestrator.provideClarification(
                    run.getRunId(),
                    "Fix a bug in the existing URL expiration validation"
            );

            waitUntil(() ->
                    run.getStatus()
                            == WorkflowStatus.WAITING_FOR_APPROVAL);

            assertEquals(
                    "Something may be wrong with our existing URL service",
                    run.getRequirement()
            );

            assertEquals(
                    "Fix a bug in the existing URL expiration validation",
                    run.getClarification()
            );

        } finally {
            orchestrator.shutdown();
        }
    }

    private void waitUntil(
            org.junit.jupiter.api.function.Executable condition)
            throws Exception {
        // Not used; see the BooleanSupplier overload below.
    }

    private void waitUntil(
            java.util.function.BooleanSupplier condition)
            throws Exception {

        assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> {
                    while (!condition.getAsBoolean()) {
                        Thread.sleep(20);
                    }
                }
        );
    }
}