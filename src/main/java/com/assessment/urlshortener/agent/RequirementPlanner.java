package com.assessment.urlshortener.agent;

import com.assessment.urlshortener.orchestration.EngineeringTask;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class RequirementPlanner {

    public ScenarioType classify(String requirement) {
        if (requirement == null || requirement.isBlank()) {
            return ScenarioType.AMBIGUOUS;
        }

        String text = requirement.toLowerCase(Locale.ROOT);

        // Explicitly ambiguous wording
        if (text.contains("unclear")
                || text.contains("not sure")
                || text.contains("maybe")
                || text.contains("something")) {
            return ScenarioType.AMBIGUOUS;
        }

        // Vague improvement requests without measurable targets
        boolean vagueImprovement =
                text.contains("faster")
                        || text.contains("better")
                        || text.contains("improve")
                        || text.contains("improvement")
                        || text.contains("optimize")
                        || text.contains("optimise");

        boolean measurableTarget =
                text.matches("(?s).*\\b\\d+\\s*(ms|milliseconds|seconds|rps|%|percent|users|transactions)\\b.*")
                        || text.contains("p95")
                        || text.contains("p99")
                        || text.contains("latency target")
                        || text.contains("throughput target")
                        || text.contains("response time target")
                        || text.contains("sla")
                        || text.contains("slo");

        if (vagueImprovement && !measurableTarget) {
            return ScenarioType.AMBIGUOUS;
        }

        // Existing-system changes
        if (text.contains("existing")
                || text.contains("refactor")
                || text.contains("fix")
                || text.contains("bug")
                || text.contains("enhance")
                || text.contains("update")) {
            return ScenarioType.BROWNFIELD;
        }

        return ScenarioType.GREENFIELD;
    }

    public List<EngineeringTask> createPlan(String requirement) {
        ScenarioType scenario = classify(requirement);

        if (scenario == ScenarioType.AMBIGUOUS) {
            return List.of(
                    new EngineeringTask(
                            "T1",
                            "Identify missing requirements and request clarification",
                            List.of()
                    )
            );
        }

        if (scenario == ScenarioType.BROWNFIELD) {
            return List.of(
                    new EngineeringTask(
                            "T1",
                            "Inspect existing code, APIs, data flows and tests",
                            List.of()
                    ),
                    new EngineeringTask(
                            "T2",
                            "Analyze impact and identify regression risks",
                            List.of("T1")
                    ),
                    new EngineeringTask(
                            "T3",
                            "Prepare implementation and regression test changes",
                            List.of("T2")
                    ),
                    new EngineeringTask(
                            "T4",
                            "Execute validation and regression checks",
                            List.of("T3")
                    ),
                    new EngineeringTask(
                            "T5",
                            "Approve change and release readiness",
                            List.of("T4"),
                            true
                    )
            );
        }

        return List.of(
                new EngineeringTask(
                        "T1",
                        "Normalize requirement and define acceptance criteria",
                        List.of()
                ),
                new EngineeringTask(
                        "T2",
                        "Design architecture and data model",
                        List.of("T1")
                ),
                new EngineeringTask(
                        "T3",
                        "Design API contracts",
                        List.of("T1")
                ),
                new EngineeringTask(
                        "T4",
                        "Prepare unit and integration test strategy",
                        List.of("T1")
                ),
                new EngineeringTask(
                        "T5",
                        "Validate implementation and release readiness",
                        List.of("T2", "T3", "T4"),
                        true
                )
        );
    }

    public List<EngineeringTask> createPlanAfterClarification(
            String originalRequirement,
            String clarification) {

        if (clarification == null || clarification.isBlank()) {
            throw new IllegalArgumentException(
                    "Clarification must not be blank");
        }

        ScenarioType resolvedScenario = classify(clarification);

        if (resolvedScenario == ScenarioType.AMBIGUOUS) {
            throw new IllegalArgumentException(
                    "The clarification is still ambiguous");
        }

        // The original requirement remains stored in WorkflowRun.
        // The clarified input determines the new plan.
        return createPlan(clarification);
    }
}