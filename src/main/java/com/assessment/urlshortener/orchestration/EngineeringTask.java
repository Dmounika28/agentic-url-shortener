
package com.assessment.urlshortener.orchestration;

import java.util.ArrayList;
import java.util.List;

public class EngineeringTask {

    private final String id;
    private final String description;
    private final List<String> dependencies;
    private final boolean requiresApproval;

    private volatile TaskStatus status = TaskStatus.PENDING;
    private volatile int attempts;
    private volatile String output;
    private volatile String error;
    private volatile Boolean approved;

    public EngineeringTask(
            String id,
            String description,
            List<String> dependencies) {
        this(id, description, dependencies, false);
    }

    public EngineeringTask(
            String id,
            String description,
            List<String> dependencies,
            boolean requiresApproval) {
        this.id = id;
        this.description = description;
        this.dependencies = new ArrayList<>(dependencies);
        this.requiresApproval = requiresApproval;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getDependencies() {
        return List.copyOf(dependencies);
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void incrementAttempts() {
        attempts++;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }
}
