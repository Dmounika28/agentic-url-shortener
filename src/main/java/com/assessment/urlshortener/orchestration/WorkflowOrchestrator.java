package com.assessment.urlshortener.orchestration;

import com.assessment.urlshortener.agent.RequirementPlanner;
import com.assessment.urlshortener.agent.ScenarioType;
import com.assessment.urlshortener.approval.ApprovalService;
import com.assessment.urlshortener.audit.AuditService;
import com.assessment.urlshortener.policy.PolicyDecision;
import com.assessment.urlshortener.policy.PolicyService;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class WorkflowOrchestrator {

    private static final int MAX_ATTEMPTS = 2;

    private final TaskExecutor taskExecutor;
    private final RequirementPlanner requirementPlanner;
    private final PolicyService policyService;
    private final AuditService auditService;
    private final ApprovalService approvalService;

    private final ExecutorService coordinator =
            Executors.newFixedThreadPool(2);

    private final ExecutorService taskPool =
            Executors.newFixedThreadPool(4);

    private final Map<String, WorkflowRun> runs =
            new ConcurrentHashMap<>();

    public WorkflowOrchestrator(
            TaskExecutor taskExecutor,
            RequirementPlanner requirementPlanner,
            PolicyService policyService,
            AuditService auditService,
            ApprovalService approvalService) {
        this.taskExecutor = taskExecutor;
        this.requirementPlanner = requirementPlanner;
        this.policyService = policyService;
        this.auditService = auditService;
        this.approvalService = approvalService;
    }

    public WorkflowRun start(String requirement) {
        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException(
                    "Requirement must not be blank");
        }

        ScenarioType scenario = requirementPlanner.classify(requirement);
        List<EngineeringTask> plan =
                requirementPlanner.createPlan(requirement);

        WorkflowRun run = new WorkflowRun(requirement, plan);

        validateGraph(plan);
        runs.put(run.getRunId(), run);

        auditService.record(
                run.getRunId(),
                null,
                "WORKFLOW_STARTED",
                "Scenario: " + scenario
        );

        if (scenario == ScenarioType.AMBIGUOUS) {
            run.setStatus(WorkflowStatus.NEEDS_CLARIFICATION);
            auditService.record(
                    run.getRunId(),
                    null,
                    "CLARIFICATION_REQUIRED",
                    "Requirement needs clarification before planning"
            );
            return run;
        }

        coordinator.execute(() -> execute(run));
        return run;
    }

    public WorkflowRun getRun(String runId) {
        WorkflowRun run = runs.get(runId);

        if (run == null) {
            throw new NoSuchElementException(
                    "Workflow not found: " + runId);
        }

        return run;
    }

    public Collection<WorkflowRun> getAllRuns() {
        return List.copyOf(runs.values());
    }

    public WorkflowRun decideApproval(
            String runId,
            String taskId,
            boolean approved) {

        WorkflowRun run = getRun(runId);

        synchronized (run) {
            EngineeringTask task = run.getTasks().stream()
                    .filter(t -> t.getId().equals(taskId))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException(
                            "Task not found: " + taskId));

            if (task.getStatus() != TaskStatus.WAITING_FOR_APPROVAL) {
                throw new IllegalStateException(
                        "Task is not waiting for approval");
            }

            approvalService.recordDecision(runId, task, approved);

            if (approved) {
                task.setStatus(TaskStatus.PENDING);
                run.setStatus(WorkflowStatus.RUNNING);
            } else {
                task.setStatus(TaskStatus.REJECTED);
                auditService.record(
                        runId,
                        taskId,
                        "TASK_REJECTED",
                        "Human approval rejected the task"
                );
            }
        }

        coordinator.execute(() -> execute(run));
        return run;
    }

    public WorkflowRun provideClarification(
            String runId,
            String clarification) {

        if (clarification == null || clarification.isBlank()) {
            throw new IllegalArgumentException(
                    "Clarification must not be blank");
        }

        WorkflowRun run = getRun(runId);

        synchronized (run) {
            if (run.getStatus() != WorkflowStatus.NEEDS_CLARIFICATION) {
                throw new IllegalStateException(
                        "Workflow is not waiting for clarification");
            }

            run.setClarification(clarification);

            // The planner uses the clarification as the resolved input.
            List<EngineeringTask> plan =
                    requirementPlanner.createPlanAfterClarification(
                            run.getRequirement(), clarification);

            validateGraph(plan);
            run.replaceTasks(plan);
            run.setStatus(WorkflowStatus.RUNNING);

            auditService.record(
                    runId,
                    null,
                    "CLARIFICATION_RECEIVED",
                    "Clarification recorded; plan regenerated"
            );
        }

        coordinator.execute(() -> execute(run));
        return run;
    }

    private void execute(WorkflowRun run) {
        synchronized (run) {
            if (run.getStatus() == WorkflowStatus.COMPLETED
                    || run.getStatus() == WorkflowStatus.FAILED
                    || run.getStatus() == WorkflowStatus.BLOCKED
                    || run.getStatus()
                    == WorkflowStatus.NEEDS_CLARIFICATION) {
                return;
            }

            run.setStatus(WorkflowStatus.RUNNING);
        }

        try {
            while (true) {
                List<EngineeringTask> ready;

                synchronized (run) {
                    List<EngineeringTask> tasks = run.getTasks();

                    if (tasks.stream().anyMatch(t ->
                            t.getStatus()
                                    == TaskStatus.WAITING_FOR_APPROVAL)) {
                        run.setStatus(WorkflowStatus.WAITING_FOR_APPROVAL);
                        return;
                    }

                    Map<String, EngineeringTask> byId = new HashMap<>();
                    tasks.forEach(t -> byId.put(t.getId(), t));

                    for (EngineeringTask task : tasks) {
                        if (task.getStatus() != TaskStatus.PENDING) {
                            continue;
                        }

                        boolean failedDependency =
                                task.getDependencies().stream()
                                        .map(byId::get)
                                        .anyMatch(dep ->
                                                dep == null
                                                        || dep.getStatus() == TaskStatus.FAILED
                                                        || dep.getStatus() == TaskStatus.BLOCKED
                                                        || dep.getStatus() == TaskStatus.REJECTED);

                        if (failedDependency) {
                            task.setStatus(TaskStatus.BLOCKED);
                            auditService.record(
                                    run.getRunId(),
                                    task.getId(),
                                    "TASK_BLOCKED",
                                    "A dependency failed or was rejected"
                            );
                        }
                    }

                    ready = tasks.stream()
                            .filter(t -> t.getStatus() == TaskStatus.PENDING)
                            .filter(t -> t.getDependencies().stream()
                                    .allMatch(id ->
                                            byId.get(id).getStatus()
                                                    == TaskStatus.COMPLETED))
                            .toList();

                    for (EngineeringTask task : ready) {
                        if (task.isRequiresApproval()
                                && task.getApproved() == null) {
                            task.setStatus(TaskStatus.WAITING_FOR_APPROVAL);
                            run.setStatus(
                                    WorkflowStatus.WAITING_FOR_APPROVAL);

                            auditService.record(
                                    run.getRunId(),
                                    task.getId(),
                                    "APPROVAL_REQUESTED",
                                    task.getDescription()
                            );
                            return;
                        }
                    }

                    ready = ready.stream()
                            .filter(t -> t.getStatus() == TaskStatus.PENDING)
                            .toList();

                    if (ready.isEmpty()) {
                        boolean unfinished = tasks.stream().anyMatch(t ->
                                t.getStatus() == TaskStatus.PENDING
                                        || t.getStatus() == TaskStatus.READY
                                        || t.getStatus() == TaskStatus.RUNNING);

                        if (unfinished) {
                            throw new IllegalStateException(
                                    "Workflow cannot progress");
                        }

                        boolean failed = tasks.stream().anyMatch(t ->
                                t.getStatus() == TaskStatus.FAILED
                                        || t.getStatus() == TaskStatus.BLOCKED
                                        || t.getStatus() == TaskStatus.REJECTED);

                        run.setStatus(failed
                                ? WorkflowStatus.FAILED
                                : WorkflowStatus.COMPLETED);
                        run.markCompleted();

                        auditService.record(
                                run.getRunId(),
                                null,
                                "WORKFLOW_FINISHED",
                                "Final status: " + run.getStatus()
                        );
                        return;
                    }

                    ready.forEach(t -> t.setStatus(TaskStatus.READY));
                }

                // Crucially, task execution is outside the workflow lock.
                executeReadyTasks(run, ready);
            }
        } catch (RuntimeException exception) {
            synchronized (run) {
                run.setStatus(WorkflowStatus.FAILED);
                run.markCompleted();
            }

            auditService.record(
                    run.getRunId(),
                    null,
                    "WORKFLOW_FAILED",
                    exception.getMessage() == null
                            ? exception.getClass().getSimpleName()
                            : exception.getMessage()
            );
        }
    }

    private void executeReadyTasks(
            WorkflowRun run,
            List<EngineeringTask> ready) {

        List<CompletableFuture<Void>> futures = ready.stream()
                .map(task -> CompletableFuture.runAsync(
                        () -> executeTask(run, task), taskPool))
                .toList();

        CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        ).join();
    }

    private void executeTask(
            WorkflowRun run,
            EngineeringTask task) {

        PolicyDecision decision = policyService.evaluate(task);

        auditService.record(
                run.getRunId(),
                task.getId(),
                "POLICY_EVALUATED",
                decision.reason()
        );

        if (!decision.allowed()) {
            task.setError(decision.reason());
            task.setStatus(TaskStatus.FAILED);

            auditService.record(
                    run.getRunId(),
                    task.getId(),
                    "POLICY_BLOCKED",
                    decision.reason()
            );
            return;
        }

        task.setStatus(TaskStatus.RUNNING);
        auditService.record(
                run.getRunId(),
                task.getId(),
                "TASK_STARTED",
                task.getDescription()
        );

        while (task.getAttempts() < MAX_ATTEMPTS) {
            task.incrementAttempts();

            auditService.record(
                    run.getRunId(),
                    task.getId(),
                    "TASK_ATTEMPT",
                    "Attempt " + task.getAttempts()
            );

            try {
                String output = taskExecutor.execute(task);

                task.setOutput(output);
                task.setError(null);
                task.setStatus(TaskStatus.COMPLETED);

                auditService.record(
                        run.getRunId(),
                        task.getId(),
                        "TASK_COMPLETED",
                        output
                );
                return;

            } catch (Exception exception) {
                String error = exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();

                task.setError(error);

                auditService.record(
                        run.getRunId(),
                        task.getId(),
                        "TASK_ATTEMPT_FAILED",
                        error
                );
            }
        }

        task.setStatus(TaskStatus.FAILED);

        auditService.record(
                run.getRunId(),
                task.getId(),
                "TASK_FAILED",
                task.getError()
        );
    }

    private void validateGraph(List<EngineeringTask> tasks) {
        Map<String, EngineeringTask> byId = new HashMap<>();

        for (EngineeringTask task : tasks) {
            if (byId.put(task.getId(), task) != null) {
                throw new IllegalArgumentException(
                        "Duplicate task ID: " + task.getId());
            }
        }

        for (EngineeringTask task : tasks) {
            for (String dependency : task.getDependencies()) {
                if (!byId.containsKey(dependency)) {
                    throw new IllegalArgumentException(
                            "Unknown dependency: " + dependency);
                }
            }
        }

        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (EngineeringTask task : tasks) {
            detectCycle(task.getId(), byId, visited, visiting);
        }
    }

    private void detectCycle(
            String id,
            Map<String, EngineeringTask> byId,
            Set<String> visited,
            Set<String> visiting) {

        if (visiting.contains(id)) {
            throw new IllegalArgumentException(
                    "Cycle detected at task: " + id);
        }

        if (visited.contains(id)) {
            return;
        }

        visiting.add(id);

        for (String dependency : byId.get(id).getDependencies()) {
            detectCycle(dependency, byId, visited, visiting);
        }

        visiting.remove(id);
        visited.add(id);
    }

    @PreDestroy
    public void shutdown() {
        coordinator.shutdownNow();
        taskPool.shutdownNow();
    }
}