package com.kairos.context_engine.domain.model.retrieval.candidate;

import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;

public record TripleCandidate(
        KnowledgeTriple triple,
        double similarityScore
) {
    public TripleCandidate {
        if (triple == null) {
            throw new IllegalArgumentException("Triple candidate triple cannot be null");
        }
        if (!Double.isFinite(similarityScore)) {
            throw new IllegalArgumentException("Triple candidate similarityScore must be finite");
        }
    }
}
