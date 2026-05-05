package com.kairos.context_engine.infrastructure.graph.repository.projection;

public record PassageScoringResult(
        String chunkId,
        double score
) {}