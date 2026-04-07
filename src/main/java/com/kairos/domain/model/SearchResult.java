package com.kairos.domain.model;

import java.util.List;

public record SearchResult(
        List<Chunk> chunks,
        List<KnowledgeTriple> knowledgeTriples
) {
}
