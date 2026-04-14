package com.kairos.presentation.dto.response;

import com.kairos.domain.model.KnowledgeTriple;

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
                triple.chunkId()
        );
    }

}