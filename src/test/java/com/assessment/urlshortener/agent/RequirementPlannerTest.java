package com.assessment.urlshortener.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequirementPlannerTest {

    private final RequirementPlanner planner = new RequirementPlanner();

    @Test
    void shouldClassifyVaguePerformanceRequestAsAmbiguous() {
        assertEquals(
                ScenarioType.AMBIGUOUS,
                planner.classify("Make the URL shortener faster and better.")
        );
    }

    @Test
    void shouldClassifyExistingSystemEnhancementAsBrownfield() {
        assertEquals(
                ScenarioType.BROWNFIELD,
                planner.classify(
                        "Enhance the existing URL shortener and preserve redirects."
                )
        );
    }

    @Test
    void shouldClassifySpecificNewFeatureAsGreenfield() {
        assertEquals(
                ScenarioType.GREENFIELD,
                planner.classify(
                        "Build a new URL shortener with HTTP APIs."
                )
        );
    }
}