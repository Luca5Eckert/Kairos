package com.kairos.context_engine.presentation.dto.response;

import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;

import java.util.UUID;

public record KnowledgeTripleResponse(
        String subject,
        String predicate,
        String object,
        UUID chunkId
) {

    public static KnowledgeTripleResponse of(KnowledgeTriple triple) {
        return new KnowledgeTripleResponse(
                triple.subject().name(),
                triple.predicate(),
                triple.object().name(),
                triple.passage().chunkId()
        );
    }

}
