package com.kairos.context_engine.infrastructure.ai.gemini.dto;

public record ExtractedTriple(
        String subject,
        String predicate,
        String object,
        double weight
) {
}
