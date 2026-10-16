package com.assessment.urlshortener.orchestration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class WorkflowRun {

    private final String runId = UUID.randomUUID().toString();
    private final String requirement;
    private volatile String clarification;
    private volatile List<EngineeringTask> tasks;
    private final Instant createdAt = Instant.now();

    private volatile WorkflowStatus status = WorkflowStatus.RUNNING;
    private volatile Instant completedAt;

    public WorkflowRun(
            String requirement,
            List<EngineeringTask> tasks) {
        this.requirement = requirement;
        this.tasks = List.copyOf(tasks);
    }

    public String getRunId() {
        return runId;
    }

    public String getRequirement() {
        return requirement;
    }

    public String getClarification() {
        return clarification;
    }

    public List<EngineeringTask> getTasks() {
        return tasks;
    }

    public void replaceTasks(List<EngineeringTask> tasks) {
        this.tasks = List.copyOf(tasks);
    }

    public void setClarification(String clarification) {
        this.clarification = clarification;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public void markCompleted() {
        this.completedAt = Instant.now();
    }
}