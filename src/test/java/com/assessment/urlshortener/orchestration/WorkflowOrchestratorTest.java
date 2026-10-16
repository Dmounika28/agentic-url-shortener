package com.assessment.urlshortener.orchestration;

import com.assessment.urlshortener.agent.RequirementPlanner;
import com.assessment.urlshortener.approval.ApprovalService;
import com.assessment.urlshortener.audit.AuditService;
import com.assessment.urlshortener.policy.PolicyService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowOrchestratorTest {

    @Test
    void shouldRetryFailedTaskAndBlockDependentTasks() {

        TaskExecutor failingExecutor = task -> {
            if ("T2".equals(task.getId())) {
                throw new RuntimeException(
                        "Simulated data model failure");
            }

            return "Completed: " + task.getDescription();
        };

        AuditService auditService = new AuditService();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        failingExecutor,
                        new RequirementPlanner(),
                        new PolicyService(),
                        auditService,
                        new ApprovalService(auditService)
                );

        try {
            WorkflowRun run = orchestrator.start(
                    "Add expiration support to the URL shortener"
            );

            assertTimeoutPreemptively(
                    Duration.ofSeconds(5),
                    () -> {
                        while (run.getStatus()
                                != WorkflowStatus.FAILED) {
                            Thread.sleep(20);
                        }
                    }
            );

            EngineeringTask t1 = findTask(run, "T1");
            EngineeringTask t2 = findTask(run, "T2");
            EngineeringTask t3 = findTask(run, "T3");
            EngineeringTask t4 = findTask(run, "T4");
            EngineeringTask t5 = findTask(run, "T5");

            assertEquals(TaskStatus.COMPLETED, t1.getStatus());

            assertEquals(TaskStatus.FAILED, t2.getStatus());
            assertEquals(2, t2.getAttempts());
            assertTrue(t2.getError().contains(
                    "Simulated data model failure"));

            assertEquals(TaskStatus.COMPLETED, t3.getStatus());
            assertEquals(TaskStatus.COMPLETED, t4.getStatus());

            assertEquals(TaskStatus.BLOCKED, t5.getStatus());
            assertEquals(0, t5.getAttempts());

            assertEquals(WorkflowStatus.FAILED, run.getStatus());

            System.out.println("Workflow status: " + run.getStatus());

            for (EngineeringTask task : run.getTasks()) {
                System.out.println(
                        task.getId()
                                + " | Status: " + task.getStatus()
                                + " | Attempts: " + task.getAttempts()
                                + " | Error: " + task.getError()
                );
            }

        } finally {
            orchestrator.shutdown();
        }
    }

    private EngineeringTask findTask(
            WorkflowRun run, String id) {

        return run.getTasks()
                .stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}