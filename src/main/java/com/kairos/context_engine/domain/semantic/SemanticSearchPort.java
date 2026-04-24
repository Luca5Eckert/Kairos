package com.kairos.context_engine.domain.semantic;

import com.kairos.context_engine.domain.model.Chunk;

import java.util.List;
import java.util.UUID;

public interface SemanticSearchPort {

    List<Chunk> findTopK(float[] queryVector, int k);

    List<Chunk> findChunks(List<UUID> triples);
}
