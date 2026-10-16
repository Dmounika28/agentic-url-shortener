package com.assessment.urlshortener.audit;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AuditService {

    private final List<AuditEvent> events =
            new CopyOnWriteArrayList<>();

    public void record(
            String runId,
            String taskId,
            String eventType,
            String details) {

        events.add(new AuditEvent(
                runId,
                taskId,
                eventType,
                details,
                Instant.now()
        ));
    }

    public List<AuditEvent> getEvents(String runId) {
        return events.stream()
                .filter(event -> event.runId().equals(runId))
                .toList();
    }

    public List<AuditEvent> getAllEvents() {
        return List.copyOf(events);
    }
}