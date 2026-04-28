package com.kairos.context_engine.domain.port.semantic;

import com.kairos.context_engine.domain.model.content.Chunk;

import java.util.List;
import java.util.UUID;

public interface SemanticSearch {

    List<Chunk> findTopK(float[] queryVector, int k);

    List<Chunk> findChunks(List<UUID> triples);
}
