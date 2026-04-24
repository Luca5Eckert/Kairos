package com.kairos.context_engine.infrastructure.embedding.factory;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;

/**
 * Factory for creating {@link OnnxTensor} instances from primitive arrays.
 *
 * <p>Wraps the static  calls behind an interface
 * so that tensor creation can be substituted in unit tests without requiring
 * a live ORT native environment.
 */
public interface TensorFactory {

    /**
     * Creates a tensor from a 2-D {@code long} array.
     *
     * @param environment the ORT environment
     * @param data        the source data, typically shaped {@code [1][sequenceLength]}
     * @return a new {@link OnnxTensor}
     * @throws OrtException if tensor creation fails
     */
    OnnxTensor createLongTensor(OrtEnvironment environment, long[][] data) throws OrtException;
}