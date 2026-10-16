package com.assessment.urlshortener.orchestration;

public enum TaskStatus {
    PENDING,
    READY,
    RUNNING,
    COMPLETED,
    FAILED,
    BLOCKED,
    WAITING_FOR_APPROVAL,
    REJECTED
}