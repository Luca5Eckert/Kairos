package com.kairos.domain.port;

import com.kairos.domain.model.Chunk;

import java.util.List;
import java.util.UUID;

public interface ChunkRepository {
    Chunk save(Chunk chunk);

    List<Chunk> findBySourceId(UUID sourceId);
}
