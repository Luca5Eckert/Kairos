package com.kairos.module.context_engine.infrastructure.graph.repository.projection;

public record PassageScoringResult(
        String chunkId,
        double score
) {}