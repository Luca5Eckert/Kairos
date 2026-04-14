package com.kairos.presentation.dto.response;

import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.KnowledgeTriple;

import java.util.List;

public record ContextResponse(
    List<KnowledgeTripleResponse> knowledgeGraph,
    List<ChunkContextResponse> chunkContexts
) {

    public static ContextResponse of(
            List<KnowledgeTriple> knowledgeTriples,
            List<Chunk> chunks
    ) {
        List<KnowledgeTripleResponse> knowledgeTripleResponses = knowledgeTriples.stream()
                .map(KnowledgeTripleResponse::of)
                .toList();

        List<ChunkContextResponse> chunkContextResponses = chunks.stream()
                .map(ChunkContextResponse::of)
                .toList();

        return new ContextResponse(knowledgeTripleResponses, chunkContextResponses);
    }


}
