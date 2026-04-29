package com.kairos.context_engine.domain.model.knowledge;

import com.kairos.context_engine.domain.model.Triple;

import java.util.UUID;

public record KnowledgeTriple(
        Concept subject,
        String predicate,
        Concept object,
        UUID chunkId,
        double weight
) {
    public static KnowledgeTriple create(Triple triple, UUID chunkId) {
        return new KnowledgeTriple(
                Concept.create(triple.subject()),
                triple.predicate(),
                Concept.create(triple.object()),
                chunkId,
                triple.weight()
        );
    }

    public static KnowledgeTriple create(String subject, String predicate, String object, UUID chunkId, double weight) {
        return new KnowledgeTriple(
                Concept.create(subject),
                predicate,
                Concept.create(object),
                chunkId,
                weight
        );
    }
}
