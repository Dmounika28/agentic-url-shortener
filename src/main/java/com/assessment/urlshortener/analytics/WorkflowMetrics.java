package com.assessment.urlshortener.analytics;

public record WorkflowMetrics(
        long totalWorkflows,
        long successfulWorkflows,
        long failedWorkflows,
        double successRatePercent,
        long totalTaskAttempts,
        long retryCount,
        double averageLatencyMillis
) {
}