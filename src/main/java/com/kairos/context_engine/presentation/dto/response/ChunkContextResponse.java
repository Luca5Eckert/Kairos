package com.kairos.context_engine.presentation.dto.response;

import com.kairos.context_engine.domain.model.content.Chunk;

import java.util.UUID;

public record ChunkContextResponse(
        UUID chunkId,
        String content
) {

    public static ChunkContextResponse of(Chunk chunk) {
        return new ChunkContextResponse(
                chunk.getId(),
                chunk.getContent()
        );
    }
}