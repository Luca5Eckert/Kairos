package com.kairos.context_engine.domain.model.retrieval.ranking;

import java.util.UUID;

public record ScoredPassage(
        UUID chunkId,
        double graphScore
) {
    public ScoredPassage {
        if (chunkId == null) {
            throw new IllegalArgumentException("Scored passage chunkId cannot be null");
        }
        if (!Double.isFinite(graphScore)) {
            throw new IllegalArgumentException("Scored passage graphScore must be finite");
        }
    }
}
