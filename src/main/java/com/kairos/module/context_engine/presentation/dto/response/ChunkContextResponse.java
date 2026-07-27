package com.kairos.module.context_engine.presentation.dto.response;

import com.kairos.module.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.module.context_engine.domain.model.retrieval.source.RetrievalSource;

import java.util.UUID;

public record ChunkContextResponse(
        UUID chunkId,
        String content,
        int rank,
        double score,
        RetrievalSource source
) {

    public static ChunkContextResponse of(RankedChunk rankedChunk) {
        return new ChunkContextResponse(
                rankedChunk.chunk().getId(),
                rankedChunk.chunk().getContent(),
                rankedChunk.rank(),
                rankedChunk.score(),
                rankedChunk.source()
        );
    }
}
