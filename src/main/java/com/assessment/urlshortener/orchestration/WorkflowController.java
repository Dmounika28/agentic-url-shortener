
package com.assessment.urlshortener.orchestration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowOrchestrator orchestrator;

    public WorkflowController(WorkflowOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public record StartWorkflowRequest(
            @NotBlank String requirement) {
    }

    public record ApprovalRequest(boolean approved) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowRun start(
            @Valid @RequestBody StartWorkflowRequest request) {
        return orchestrator.start(request.requirement());
    }

    @GetMapping("/{runId}")
    public WorkflowRun get(@PathVariable String runId) {
        return orchestrator.getRun(runId);
    }

    @PostMapping("/{runId}/tasks/{taskId}/approval")
    public WorkflowRun decideApproval(
            @PathVariable String runId,
            @PathVariable String taskId,
            @RequestBody ApprovalRequest request) {

        return orchestrator.decideApproval(
                runId, taskId, request.approved());
    }

    @GetMapping("/{runId}/summary")
    public Map<String, Object> summary(
            @PathVariable String runId) {

        WorkflowRun run = orchestrator.getRun(runId);

        long completed = run.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();

        long failed = run.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.FAILED)
                .count();

        long blocked = run.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.BLOCKED)
                .count();

        return Map.of(
                "runId", run.getRunId(),
                "requirement", run.getRequirement(),
                "totalTasks", run.getTasks().size(),
                "completedTasks", completed,
                "failedTasks", failed,
                "blockedTasks", blocked,
                "tasks", run.getTasks(),
                "status", run.getStatus(),
                "createdAt", run.getCreatedAt(),
                "completedAt", run.getCompletedAt() == null
                        ? ""
                        : run.getCompletedAt()
        );
    }

    public record ClarificationRequest(
            @NotBlank String clarification) {
    }

    @PostMapping("/{runId}/clarification")
    public WorkflowRun provideClarification(
            @PathVariable String runId,
            @Valid @RequestBody ClarificationRequest request) {

        return orchestrator.provideClarification(
                runId,
                request.clarification());
    }
}
