package com.kairos.context_engine.domain.model.retrieval.candidate;

import com.kairos.context_engine.domain.model.knowledge.Concept;

public record ConceptCandidate(
        Concept concept,
        double similarityScore
) {
    public ConceptCandidate {
        if (concept == null) {
            throw new IllegalArgumentException("Concept cannot be null");
        }
        if (!Double.isFinite(similarityScore)) {
            throw new IllegalArgumentException("SimilarityScore must be finite");
        }
    }
}
