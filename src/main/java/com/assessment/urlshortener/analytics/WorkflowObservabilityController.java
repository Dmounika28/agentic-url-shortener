package com.assessment.urlshortener.analytics;

import com.assessment.urlshortener.audit.AuditEvent;
import com.assessment.urlshortener.audit.AuditService;
import com.assessment.urlshortener.orchestration.WorkflowOrchestrator;
import com.assessment.urlshortener.orchestration.WorkflowRun;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/observability")
public class WorkflowObservabilityController {

    private final AuditService auditService;
    private final WorkflowAnalyticsService analyticsService;
    private final WorkflowOrchestrator orchestrator;

    public WorkflowObservabilityController(
            AuditService auditService,
            WorkflowAnalyticsService analyticsService,
            WorkflowOrchestrator orchestrator) {
        this.auditService = auditService;
        this.analyticsService = analyticsService;
        this.orchestrator = orchestrator;
    }

    @GetMapping("/workflows/{runId}/audit")
    public List<AuditEvent> audit(@PathVariable String runId) {
        orchestrator.getRun(runId); // Validate that the run exists
        return auditService.getEvents(runId);
    }

    @GetMapping("/metrics")
    public WorkflowMetrics metrics() {
        Collection<WorkflowRun> runs = orchestrator.getAllRuns();
        return analyticsService.calculate(runs);
    }
}