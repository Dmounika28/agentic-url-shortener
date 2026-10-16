package com.assessment.urlshortener.audit;

import java.time.Instant;

public record AuditEvent(
        String runId,
        String taskId,
        String eventType,
        String details,
        Instant timestamp
) {
}