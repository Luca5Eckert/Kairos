package com.kairos.domain.port;

import com.kairos.domain.model.Chunk;

public interface ChunkRepository {
    Chunk save(Chunk chunk);
}
