package com.assessment.urlshortener.policy;

import com.assessment.urlshortener.orchestration.EngineeringTask;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolicyServiceTest {

    private final PolicyService policyService = new PolicyService();

    @Test
    void shouldBlockDangerousProductionOperation() {
        EngineeringTask task = new EngineeringTask(
                "T1",
                "Delete production database",
                List.of()
        );

        PolicyDecision decision = policyService.evaluate(task);

        assertFalse(decision.allowed());
        assertTrue(decision.reason().contains("high-risk"));
    }

    @Test
    void shouldAllowNormalEngineeringTask() {
        EngineeringTask task = new EngineeringTask(
                "T1",
                "Write unit tests for URL expiration",
                List.of()
        );

        PolicyDecision decision = policyService.evaluate(task);

        assertTrue(decision.allowed());
    }

    @Test
    void shouldRequireApprovalForHighImpactTask() {
        EngineeringTask task = new EngineeringTask(
                "T1",
                "Release application to production",
                List.of(),
                true
        );

        PolicyDecision decision = policyService.evaluate(task);

        assertFalse(decision.allowed());
        assertTrue(decision.reason().contains("approval"));
    }
}