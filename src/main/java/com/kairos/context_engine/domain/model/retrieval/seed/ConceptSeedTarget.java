package com.kairos.context_engine.domain.model.retrieval.seed;

import com.kairos.context_engine.domain.model.knowledge.Concept;

public record ConceptSeedTarget(
        Concept concept
) implements GraphSeedTarget {
    public ConceptSeedTarget {
        if (concept == null) {
            throw new IllegalArgumentException("Concept seed target cannot be null");
        }
    }

    public static ConceptSeedTarget fromName(String conceptName) {
        return new ConceptSeedTarget(Concept.create(conceptName));
    }
}
