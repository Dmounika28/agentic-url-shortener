package com.assessment.urlshortener.approval;

import com.assessment.urlshortener.audit.AuditService;
import com.assessment.urlshortener.orchestration.EngineeringTask;
import org.springframework.stereotype.Service;

@Service
public class ApprovalService {

    private final AuditService auditService;

    public ApprovalService(AuditService auditService) {
        this.auditService = auditService;
    }

    public void recordDecision(
            String runId,
            EngineeringTask task,
            boolean approved) {

        task.setApproved(approved);

        auditService.record(
                runId,
                task.getId(),
                approved ? "APPROVAL_GRANTED" : "APPROVAL_REJECTED",
                "Human approval decision recorded"
        );
    }
}