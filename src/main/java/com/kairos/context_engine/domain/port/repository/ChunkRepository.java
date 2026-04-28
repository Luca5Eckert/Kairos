package com.kairos.context_engine.domain.port.repository;

import com.kairos.context_engine.domain.model.content.Chunk;

import java.util.List;
import java.util.UUID;

public interface ChunkRepository {
    Chunk save(Chunk chunk);

    List<Chunk> findAllBySourceId(UUID id);
}
