package com.kairos.context_engine.domain.model.retrieval.seed;

import java.util.UUID;

public record PassageSeedTarget(
        UUID chunkId
) implements GraphSeedTarget {
    public PassageSeedTarget {
        if (chunkId == null) {
            throw new IllegalArgumentException("Passage seed chunkId cannot be null");
        }
    }
}
