package com.kairos.context_engine.domain.port;

import com.kairos.context_engine.domain.model.Chunk;

public interface ChunkRepository {
    Chunk save(Chunk chunk);
}
