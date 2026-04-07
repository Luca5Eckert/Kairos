package com.kairos.domain.model;

import java.util.UUID;

public record KnowledgeTriple(
        Concept subject,
        String predicate,
        Concept object,
        UUID chunkId
) {
    public static KnowledgeTriple create(Triple triple, UUID chunkId) {
        return new KnowledgeTriple(
                Concept.create(triple.subject()),
                triple.predicate(),
                Concept.create(triple.object()),
                chunkId
        );
    }
}
