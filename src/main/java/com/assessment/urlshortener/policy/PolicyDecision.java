package com.assessment.urlshortener.policy;

public record PolicyDecision(
        boolean allowed,
        String reason
) {
}