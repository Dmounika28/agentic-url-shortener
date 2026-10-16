package com.assessment.urlshortener.policy;

import com.assessment.urlshortener.orchestration.EngineeringTask;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    public PolicyDecision evaluate(EngineeringTask task) {
        if (task == null) {
            return new PolicyDecision(false, "Task must not be null");
        }

        String description = task.getDescription()
                .toLowerCase();

        if (description.contains("delete production")
                || description.contains("drop production database")
                || description.contains("disable security")) {
            return new PolicyDecision(
                    false,
                    "Blocked by policy: high-risk operation"
            );
        }

        if (task.isRequiresApproval()
                && !Boolean.TRUE.equals(task.getApproved())) {
            return new PolicyDecision(
                    false,
                    "Human approval is required"
            );
        }

        return new PolicyDecision(true, "Policy checks passed");
    }
}