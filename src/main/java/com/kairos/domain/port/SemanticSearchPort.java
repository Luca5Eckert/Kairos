package com.kairos.domain.port;

import com.kairos.domain.model.Source;

import java.util.List;

public interface SemanticSearchPort {

    /**
     * Search for the most relevant sources based on the provided embedding.
     * @param embedding The embedding vector to search for.
     * @param k The number of top relevant sources to return.
     * @return A list of the most relevant sources based on the provided embedding.
     */
    List<Source> search(float[] embedding, int k);

}
