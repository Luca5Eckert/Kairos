package com.kairos.domain.semantic;

public interface EmbeddingProvider {

    float[] embed(String text);
}
