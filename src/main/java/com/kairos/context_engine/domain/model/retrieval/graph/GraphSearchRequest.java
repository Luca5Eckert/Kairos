package com.kairos.context_engine.domain.model.retrieval.graph;

import com.kairos.context_engine.domain.model.retrieval.seed.GraphSeed;

import java.util.List;

public record GraphSearchRequest(
        List<GraphSeed> seeds,
        int limit
) {
    public GraphSearchRequest {
        if (seeds == null) {
            throw new IllegalArgumentException("Graph search seeds cannot be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Graph search limit must be positive");
        }

        seeds = List.copyOf(seeds);
    }
}
