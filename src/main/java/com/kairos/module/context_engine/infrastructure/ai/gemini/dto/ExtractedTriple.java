package com.kairos.module.context_engine.infrastructure.ai.gemini.dto;

public record ExtractedTriple(
        String subject,
        String predicate,
        String object,
        Double weight
) {
}
