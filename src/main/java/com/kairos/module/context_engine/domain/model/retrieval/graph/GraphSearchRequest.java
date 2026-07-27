package com.kairos.module.context_engine.domain.model.retrieval.graph;

import com.kairos.module.context_engine.domain.model.retrieval.seed.GraphSeed;

import java.util.List;
import java.util.UUID;

public record GraphSearchRequest(
        UUID userId,
        List<GraphSeed> seeds,
        int limit
) {
    public GraphSearchRequest {
        if (userId == null) {
            throw new IllegalArgumentException("Graph search userId cannot be null");
        }
        if (seeds == null) {
            throw new IllegalArgumentException("Graph search seeds cannot be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Graph search limit must be positive");
        }

        seeds = List.copyOf(seeds);
    }

    public static GraphSearchRequest from(UUID userId, List<GraphSeed> seeds, int limit) {
        return new GraphSearchRequest(userId, seeds, limit);
    }
}
