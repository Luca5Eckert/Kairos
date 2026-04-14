package com.kairos.presentation.dto.response;

import com.kairos.domain.model.Chunk;

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