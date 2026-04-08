package com.kairos.infrastructure.embedding;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.kairos.domain.embedding.EmbeddingProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OnnxEmbeddingProvider implements EmbeddingProvider {

    private static final int MAX_SEQUENCE_LENGTH = 256;
    private static final int EMBEDDING_DIMENSION = 384;

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;

    public OnnxEmbeddingProvider(OrtEnvironment environment, OrtSession session, HuggingFaceTokenizer tokenizer) {
        this.environment = environment;
        this.session = session;
        this.tokenizer = tokenizer;
    }

    @Override
    public float[] embed(String text) {
        validateInput(text);

        TokenizedInput tokenized = tokenize(text);
        float[][] tokenEmbeddings = infer(tokenized);

        float[] pooled = meanPool(tokenEmbeddings, tokenized.attentionMask());

        return normalizeL2(pooled);
    }

    private float[] normalizeL2(float[] vector) {
        double sum = 0.0;

        for (float value : vector) {
            sum += value * value;
        }

        double norm = Math.sqrt(sum);

        if (norm == 0.0d) {
            return vector;
        }

        for (int i = 0; i < vector.length; i++) {
            vector[i] /= (float) norm;
        }

        return vector;
    }

    private float[] meanPool(float[][] tokenEmbeddings, long[] attentionMask) {
        float[] pooled = new float[EMBEDDING_DIMENSION];
        int validTokenCount = 0;

        for (int tokenIndex = 0; tokenIndex < tokenEmbeddings.length; tokenIndex++) {
            if (attentionMask[tokenIndex] == 0L) {
                continue;
            }

            validTokenCount++;

            for (int dimension = 0; dimension < EMBEDDING_DIMENSION; dimension++) {
                pooled[dimension] += tokenEmbeddings[tokenIndex][dimension];
            }
        }

        if (validTokenCount == 0) {
            return pooled;
        }

        for (int dimension = 0; dimension < EMBEDDING_DIMENSION; dimension++) {
            pooled[dimension] /= validTokenCount;
        }

        return pooled;
    }

    /**
     * Run the ONNX model inference using the tokenized input and return the token embeddings.
     * @param tokenized The tokenized input containing input IDs, attention mask, and token type IDs.
     * @return A 2D float array where each row corresponds to the embedding of a token in the input sequence. The shape of the output is [sequence_length, embedding_dimension].
     */
    private float[][] infer(TokenizedInput tokenized) {
        long[][] inputIds = new long[][] { tokenized.inputIds() };
        long[][] attentionMask = new long[][] { tokenized.attentionMask() };
        long[][] tokenTypeIds = new long[][] { tokenized.tokenTypeIds() };

        try (
                OnnxTensor inputIdsTensor = OnnxTensor.createTensor(environment, inputIds);
                OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(environment, attentionMask);
                OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(environment, tokenTypeIds)
        ) {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);
            inputs.put("token_type_ids", tokenTypeIdsTensor);

            try (OrtSession.Result result = session.run(inputs)) {
                float[][][] output = (float[][][]) result.get(0).getValue();
                return output[0];
            }
        } catch (OrtException e) {
            throw new IllegalStateException("Failed to run embedding inference", e);
        }
    }

    /**
     * Tokenize the input text using the tokenizer.
     * @param text The input text to be tokenized.
     * @return the tokenized input.
     */
    private TokenizedInput tokenize(String text) {
        var encoding = tokenizer.encode(text);

        long[] inputIds = toLongArray(encoding.getIds());
        long[] attentionMask = toLongArray(encoding.getAttentionMask());
        long[] tokenTypeIds = toLongArray(encoding.getTypeIds());

        inputIds = truncate(inputIds, MAX_SEQUENCE_LENGTH);
        attentionMask = truncate(attentionMask, MAX_SEQUENCE_LENGTH);
        tokenTypeIds = truncate(tokenTypeIds, MAX_SEQUENCE_LENGTH);

        return new TokenizedInput(inputIds, attentionMask, tokenTypeIds);
    }

    /**
     * Truncate the input array to the specified maximum sequence length.
     * @param values The input array to be truncated.
     * @param maxLength The maximum allowed sequence length. If the input array is longer than this, it will be truncated to fit.
     * @return A new array containing only the first maxSequenceLength elements of the input array, or the original array if it is already within the limit.
     */
    private long[] truncate(long[] values, int maxLength) {
        if (values.length <= maxLength) {
            return values;
        }
        return Arrays.copyOf(values, maxLength);
    }


    private long[] toLongArray(long[] ids) {
        return ids;
    }

    private void validateInput(String text) {
        if ( text == null || text.isBlank() || text.length() > MAX_SEQUENCE_LENGTH) {
            throw new IllegalArgumentException("Input text must be non-empty and at most " + MAX_SEQUENCE_LENGTH + " characters long.");
        }
    }
}
