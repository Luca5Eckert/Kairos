package com.kairos.domain.embedding;

import com.kairos.domain.model.Triple;

import java.util.List;

public interface EmbeddingProvider {

    /**
     * Generate an embedding vector for the given text.
     * @param text The input text to be embedded.
     * @return A float array representing the embedding vector for the input text.
     */
    float[] embed(String text);

}
