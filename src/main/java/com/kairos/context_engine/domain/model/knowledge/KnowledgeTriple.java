package com.kairos.context_engine.domain.model.knowledge;

import com.kairos.context_engine.domain.model.Triple;

public record KnowledgeTriple(
        Concept subject,
        String predicate,
        Concept object,
        Passage passage,
        double weight
) {
    public static KnowledgeTriple create(Triple triple, Passage passage) {
        return new KnowledgeTriple(
                Concept.create(triple.subject()),
                triple.predicate(),
                Concept.create(triple.object()),
                passage,
                triple.weight()
        );
    }

    public static KnowledgeTriple create(String subject, String predicate, String object, Passage passage, double weight) {
        return new KnowledgeTriple(
                Concept.create(subject),
                predicate,
                Concept.create(object),
                passage,
                weight
        );
    }
}
