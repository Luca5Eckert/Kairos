package com.kairos.context_engine.domain.model.retrieval.candidate;

import java.util.UUID;

public record PassageCandidate(
        UUID chunkId,
        double denseScore
) {
    public PassageCandidate {
        if (chunkId == null) {
            throw new IllegalArgumentException("Passage candidate chunkId cannot be null");
        }
        if (!Double.isFinite(denseScore)) {
            throw new IllegalArgumentException("Passage candidate denseScore must be finite");
        }
    }
}
