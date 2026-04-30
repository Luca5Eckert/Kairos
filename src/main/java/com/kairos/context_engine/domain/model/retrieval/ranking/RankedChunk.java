package com.kairos.context_engine.domain.model.retrieval.ranking;

import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.retrieval.source.RetrievalSource;

public record RankedChunk(
        Chunk chunk,
        int rank,
        double score,
        RetrievalSource source
) {
    public RankedChunk {
        if (chunk == null) {
            throw new IllegalArgumentException("Ranked chunk cannot be null");
        }
        if (rank <= 0) {
            throw new IllegalArgumentException("Ranked chunk rank must be positive");
        }
        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException("Ranked chunk score must be finite");
        }
        if (source == null) {
            throw new IllegalArgumentException("Ranked chunk source cannot be null");
        }
    }
}
