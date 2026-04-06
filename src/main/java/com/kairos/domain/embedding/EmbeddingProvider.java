package com.kairos.domain.embedding;

public interface EmbeddingProvider {

    float[] embed(String text);
}
