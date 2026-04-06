package com.kairos.domain.port;

import com.kairos.domain.model.Chunk;

public interface ChunkRepository {
    void save(Chunk chunk);
}
