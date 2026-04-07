package com.kairos.domain.model;

public record KnowledgeTriple(
        Concept subject,
        String predicate,
        Concept object,
        UUID chunkId
) {
}
