package com.kairos.context_engine.domain.model;

public record Triple(
        String subject,
        String predicate,
        String object
) {
}
