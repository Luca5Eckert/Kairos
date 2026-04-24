package com.kairos.context_engine.domain.model;

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

    public static KnowledgeTriple create(String subject, String predicate, String object, UUID chunkId) {
        return new KnowledgeTriple(
                Concept.create(subject),
                predicate,
                Concept.create(object),
                chunkId
        );
    }
}
