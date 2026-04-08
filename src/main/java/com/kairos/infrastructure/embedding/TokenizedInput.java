package com.kairos.infrastructure.embedding;

public record TokenizedInput(
        long[] inputIds,
        long[] attentionMask,
        long[] tokenTypeIds
) {
}
