package com.assessment.urlshortener.orchestration;

public enum WorkflowStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    BLOCKED,
    WAITING_FOR_APPROVAL,
    NEEDS_CLARIFICATION
}