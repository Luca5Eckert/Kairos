package com.kairos.domain.semantic;

import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.Source;

import java.util.List;
import java.util.UUID;

public interface SemanticSearchPort {

    /**
     * Search for the most relevant sources based on the provided embedding.
     * @param embedding The embedding vector to search for.
     * @param k The number of top relevant sources to return.
     * @return A list of the most relevant sources based on the provided embedding.
     */
    List<Source> search(float[] embedding, int k);

    List<Chunk> findTopK(float[] queryVector, int i);

    List<Chunk> findChunks(List<UUID> triples);
}
