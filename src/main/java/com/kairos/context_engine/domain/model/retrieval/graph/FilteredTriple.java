package com.kairos.context_engine.domain.model.retrieval.graph;

import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.context_engine.domain.model.retrieval.candidate.TripleCandidate;

public record FilteredTriple(
        TripleCandidate candidate,
        boolean accepted
) {
    public FilteredTriple {
        if (candidate == null) {
            throw new IllegalArgumentException("Filtered triple candidate cannot be null");
        }
    }

    public KnowledgeTriple triple() {
        return candidate.triple();
    }

    public double similarityScore() {
        return candidate.similarityScore();
    }
}
