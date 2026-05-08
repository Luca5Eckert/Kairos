package com.kairos.context_engine.infrastructure.ai.gemini.dto;

import java.util.List;

public record TripleExtractionResult(
        List<ExtractedTriple> triples
) {
    public List<ExtractedTriple> triplesOrEmpty() {
        return triples == null ? List.of() : triples;
    }
}