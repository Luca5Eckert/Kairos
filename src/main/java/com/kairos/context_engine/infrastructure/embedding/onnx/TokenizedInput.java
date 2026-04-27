package com.kairos.context_engine.infrastructure.embedding.onnx;

public record TokenizedInput(
        long[] inputIds,
        long[] attentionMask,
        long[] tokenTypeIds
) {
}
