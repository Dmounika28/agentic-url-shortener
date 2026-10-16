package com.assessment.urlshortener.analytics;

import com.assessment.urlshortener.orchestration.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Service
public class WorkflowAnalyticsService {

    public WorkflowMetrics calculate(
            Collection<WorkflowRun> runs) {

        long total = runs.size();

        long successful = runs.stream()
                .filter(r -> r.getStatus() == WorkflowStatus.COMPLETED)
                .count();

        long failed = runs.stream()
                .filter(r -> r.getStatus() == WorkflowStatus.FAILED)
                .count();

        double successRate = total == 0
                ? 0.0
                : successful * 100.0 / total;

        List<EngineeringTask> tasks = runs.stream()
                .flatMap(r -> r.getTasks().stream())
                .toList();

        long attempts = tasks.stream()
                .mapToLong(EngineeringTask::getAttempts)
                .sum();

        long retryCount = tasks.stream()
                .mapToLong(t -> Math.max(0, t.getAttempts() - 1))
                .sum();

        double averageLatency = runs.stream()
                .filter(r -> r.getCompletedAt() != null)
                .mapToLong(r -> Duration.between(
                        r.getCreatedAt(),
                        r.getCompletedAt()
                ).toMillis())
                .average()
                .orElse(0.0);

        return new WorkflowMetrics(
                total,
                successful,
                failed,
                successRate,
                attempts,
                retryCount,
                averageLatency
        );
    }
}