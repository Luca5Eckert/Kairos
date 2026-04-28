package com.kairos.context_engine.domain.model;

import com.kairos.context_engine.domain.model.content.Chunk;

import java.util.List;

public record SearchResult(
        List<Chunk> chunks,
        List<KnowledgeTriple> knowledgeTriples
) {
    public static SearchResult from(List<KnowledgeTriple> triples, List<Chunk> expandedContext) {
        return new SearchResult(expandedContext, triples);
    }

    public static SearchResult empty() {
        return new SearchResult(List.of(), List.of());
    }
}
