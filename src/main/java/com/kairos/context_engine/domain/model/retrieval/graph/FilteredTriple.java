package com.kairos.context_engine.domain.model.retrieval.graph;

import com.kairos.context_engine.domain.model.knowledge.Concept;
import com.kairos.context_engine.domain.model.retrieval.candidate.ConceptCandidate;

public record FilteredTriple(
        ConceptCandidate candidate,
        boolean accepted
) {
    public FilteredTriple {
        if (candidate == null) {
            throw new IllegalArgumentException("Filtered triple candidate cannot be null");
        }
    }

    public Concept concept() {
        return candidate.concept();
    }

    public double similarityScore() {
        return candidate.similarityScore();
    }
}
