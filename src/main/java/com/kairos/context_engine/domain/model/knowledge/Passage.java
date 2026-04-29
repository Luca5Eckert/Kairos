package com.kairos.context_engine.domain.model.knowledge;

import java.util.UUID;

public record Passage(
        UUID chunkId
) {
    public Passage {
        if (chunkId == null) {
            throw new IllegalArgumentException("Passage chunkId cannot be null");
        }
    }

    public static Passage fromChunkId(UUID chunkId) {
        return new Passage(chunkId);
    }
}
