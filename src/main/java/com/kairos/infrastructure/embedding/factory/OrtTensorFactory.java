package com.kairos.infrastructure.embedding.factory;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import com.kairos.infrastructure.embedding.OnnxEmbeddingProvider;
import org.springframework.stereotype.Component;

/**
 * Production implementation of {@link TensorFactory}.
 *
 * <p>Delegates directly to , which requires a
 * live native ORT environment. This class is intentionally thin so that all
 * meaningful logic lives in {@link OnnxEmbeddingProvider} and can be tested
 * without a native runtime.
 */
@Component
public class OrtTensorFactory implements TensorFactory {

    @Override
    public OnnxTensor createLongTensor(OrtEnvironment environment, long[][] data) throws OrtException {
        return OnnxTensor.createTensor(environment, data);
    }
}