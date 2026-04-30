package com.kairos.context_engine.domain.model.retrieval.seed;

import java.util.UUID;

public record GraphSeed(
        GraphSeedTarget target,
        SeedType type,
        double weight
) {
    public GraphSeed {
        if (target == null) {
            throw new IllegalArgumentException("Graph seed target cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Graph seed type cannot be null");
        }
        if (!Double.isFinite(weight) || weight <= 0) {
            throw new IllegalArgumentException("Graph seed weight must be a positive finite value");
        }
        if (target instanceof PassageSeedTarget && type != SeedType.PASSAGE) {
            throw new IllegalArgumentException("Passage seed target must use PASSAGE seed type");
        }
        if (target instanceof ConceptSeedTarget && type != SeedType.CONCEPT) {
            throw new IllegalArgumentException("Concept seed target must use CONCEPT seed type");
        }
    }

    public static GraphSeed passage(UUID chunkId, double weight) {
        return new GraphSeed(new PassageSeedTarget(chunkId), SeedType.PASSAGE, weight);
    }

    public static GraphSeed concept(String conceptName, double weight) {
        return new GraphSeed(ConceptSeedTarget.fromName(conceptName), SeedType.CONCEPT, weight);
    }
}
