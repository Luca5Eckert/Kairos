package com.kairos.domain.semantic;

import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.Source;

import java.util.List;
import java.util.UUID;

public interface SemanticSearchPort {


    List<Chunk> findTopK(float[] queryVector, int i);

    List<Chunk> findChunks(List<UUID> triples);
}
