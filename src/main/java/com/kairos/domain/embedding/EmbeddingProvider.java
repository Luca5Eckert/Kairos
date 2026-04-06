package com.kairos.domain.embedding;

public interface EmbeddingProvider {

    /**
     * Generate an embedding vector for the given text.
     * @param text The input text to be embedded.
     * @return A float array representing the embedding vector for the input text.
     */
    float[] embed(String text);
}
