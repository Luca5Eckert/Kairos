package com.kairos.context_engine.domain.model.retrieval.candidate;

import java.util.UUID;

public record TripleCandidate(
        String key,
        String subject,
        String predicate,
        String object,
        UUID chunkId,
        double similarityScore
) {
    public TripleCandidate {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Triple candidate key cannot be null or blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Triple candidate subject cannot be null or blank");
        }
        if (predicate == null || predicate.isBlank()) {
            throw new IllegalArgumentException("Triple candidate predicate cannot be null or blank");
        }
        if (object == null || object.isBlank()) {
            throw new IllegalArgumentException("Triple candidate object cannot be null or blank");
        }
        if (chunkId == null) {
            throw new IllegalArgumentException("Triple candidate chunkId cannot be null");
        }
        if (!Double.isFinite(similarityScore)) {
            throw new IllegalArgumentException("Triple candidate similarity score must be finite");
        }

        key = key.trim();
        subject = subject.trim();
        predicate = predicate.trim();
        object = object.trim();
    }
}
