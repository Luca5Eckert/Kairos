package com.kairos.context_engine.domain.model;

public record Triple(
        String subject,
        String predicate,
        String object,
        double weight
) {
    public Triple(String subject, String predicate, String object) {
        this(subject, predicate, object, 1.0);
    }
}
