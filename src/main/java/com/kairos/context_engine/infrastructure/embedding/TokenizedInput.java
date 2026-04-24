package com.kairos.context_engine.infrastructure.embedding;

public record TokenizedInput(
        long[] inputIds,
        long[] attentionMask,
        long[] tokenTypeIds
) {
}
