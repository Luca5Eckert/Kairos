package com.kairos.context_engine.infrastructure.ai.gemini.dto;

import java.util.List;

public record TripleExtractionResult(
        List<ExtractedTriple> triples
) {
    public TripleExtractionResult {
        triples = triples == null ? List.of() : List.copyOf(triples);
    }
    public List<ExtractedTriple> triplesOrEmpty() {
        return triples;
    }

}