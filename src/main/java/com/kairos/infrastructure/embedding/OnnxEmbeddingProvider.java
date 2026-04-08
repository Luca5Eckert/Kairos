package com.kairos.infrastructure.embedding;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.kairos.domain.embedding.EmbeddingProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@link EmbeddingProvider} implementation backed by an ONNX model.
 *
 * <p>This component is responsible for:
 * <ol>
 *   <li>Validating the input text</li>
 *   <li>Tokenizing the text using a Hugging Face tokenizer</li>
 *   <li>Running inference against an ONNX Runtime session</li>
 *   <li>Applying mean pooling over valid tokens</li>
 *   <li>Applying L2 normalization to the final embedding</li>
 * </ol>
 *
 * <p><strong>Design goals</strong>:
 * <ul>
 *   <li>Avoid hard-coding model-specific assumptions when possible</li>
 *   <li>Support models that do not require {@code token_type_ids}</li>
 *   <li>Fail fast with clear error messages when the model output is unexpected</li>
 * </ul>
 *
 * <p><strong>Important note about pooling</strong>:
 * This implementation performs mean pooling over all tokens whose
 * {@code attention_mask == 1}. That usually includes special tokens such as
 * {@code [CLS]} and {@code [SEP]}. Depending on the model you use, you may want
 * to exclude them for better embedding quality.
 */
@Component
public class OnnxEmbeddingProvider implements EmbeddingProvider {

    /**
     * Maximum number of tokens sent to the model.
     *
     * <p>If the tokenizer produces more than this amount, the sequence is truncated.
     * Whether padding is necessary depends on how the ONNX model was exported.
     */
    private static final int MAX_SEQUENCE_LENGTH = 256;

    private static final String INPUT_IDS_NAME = "input_ids";
    private static final String ATTENTION_MASK_NAME = "attention_mask";
    private static final String TOKEN_TYPE_IDS_NAME = "token_type_ids";

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;

    /**
     * Creates a new provider.
     *
     * @param environment ONNX Runtime environment
     * @param session ONNX Runtime session containing the embedding model
     * @param tokenizer tokenizer compatible with the ONNX model
     */
    public OnnxEmbeddingProvider(
            OrtEnvironment environment,
            OrtSession session,
            HuggingFaceTokenizer tokenizer
    ) {
        this.environment = Objects.requireNonNull(environment, "environment cannot be null");
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer cannot be null");
    }

    /**
     * Generates a normalized embedding vector for the given text.
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Validate input</li>
     *   <li>Tokenize and truncate</li>
     *   <li>Run ONNX inference</li>
     *   <li>Mean-pool token embeddings using the attention mask</li>
     *   <li>L2-normalize the pooled vector</li>
     * </ol>
     *
     * @param text input text to embed
     * @return normalized embedding vector
     * @throws IllegalArgumentException if the input text is null or blank
     * @throws IllegalStateException if model inference fails or the model output is invalid
     */
    @Override
    public float[] embed(String text) {
        validateInput(text);

        TokenizedInput tokenizedInput = tokenize(text);
        float[][] tokenEmbeddings = infer(tokenizedInput);
        float[] pooledEmbedding = meanPool(tokenEmbeddings, tokenizedInput.attentionMask());

        return normalizeL2(pooledEmbedding);
    }

    /**
     * Tokenizes the input text and truncates all generated arrays to the maximum
     * supported sequence length.
     *
     * @param text text to tokenize
     * @return tokenized model input
     */
    private TokenizedInput tokenize(String text) {
        Encoding encoding = tokenizer.encode(text);

        long[] inputIds = truncate(encoding.getIds());
        long[] attentionMask = truncate(encoding.getAttentionMask());

        /*
         * Some tokenizers/models may not produce token type ids or may not need them.
         * We keep the array if it exists, but the inference step will only send it if
         * the ONNX session declares that input.
         */
        long[] tokenTypeIds = encoding.getTypeIds() != null
                ? truncate(encoding.getTypeIds())
                : createDefaultTokenTypeIds(inputIds.length);

        return new TokenizedInput(inputIds, attentionMask, tokenTypeIds);
    }

    /**
     * Runs ONNX inference and returns token-level embeddings.
     *
     * <p>Expected output shape:
     * <pre>
     * [batch_size, sequence_length, embedding_dimension]
     * </pre>
     *
     * <p>This method assumes batch size 1, since the provider embeds one text at a time.
     *
     * @param tokenizedInput tokenized input for the model
     * @return token embeddings for the single input text, with shape
     *         {@code [sequence_length][embedding_dimension]}
     * @throws IllegalStateException if inference fails or the output shape is invalid
     */
    private float[][] infer(TokenizedInput tokenizedInput) {
        long[][] inputIds = new long[][] { tokenizedInput.inputIds() };
        long[][] attentionMask = new long[][] { tokenizedInput.attentionMask() };
        long[][] tokenTypeIds = new long[][] { tokenizedInput.tokenTypeIds() };

        try (
                OnnxTensor inputIdsTensor = OnnxTensor.createTensor(environment, inputIds);
                OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(environment, attentionMask);
                OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(environment, tokenTypeIds)
        ) {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put(INPUT_IDS_NAME, inputIdsTensor);
            inputs.put(ATTENTION_MASK_NAME, attentionMaskTensor);

            if (expectsInput()) {
                inputs.put(TOKEN_TYPE_IDS_NAME, tokenTypeIdsTensor);
            }

            try (OrtSession.Result result = session.run(inputs)) {
                if (result.size() == 0) {
                    throw new IllegalStateException("ONNX model returned no outputs");
                }

                Object rawOutput = result.get(0).getValue();

                if (!(rawOutput instanceof float[][][] output)) {
                    throw new IllegalStateException(
                            "Unexpected ONNX output type: expected float[][][], but got "
                                    + (rawOutput == null ? "null" : rawOutput.getClass().getName())
                    );
                }

                validateModelOutput(output);

                return output[0];
            }
        } catch (OrtException e) {
            throw new IllegalStateException("Failed to run embedding inference", e);
        }
    }

    /**
     * Applies mean pooling over token embeddings using the attention mask.
     *
     * <p>Only positions where {@code attentionMask[i] == 1} are included.
     *
     * @param tokenEmbeddings token embeddings with shape {@code [sequence_length][embedding_dimension]}
     * @param attentionMask attention mask aligned with the token sequence
     * @return pooled embedding vector
     * @throws IllegalStateException if embeddings and mask lengths do not match
     */
    private float[] meanPool(float[][] tokenEmbeddings, long[] attentionMask) {
        if (tokenEmbeddings.length != attentionMask.length) {
            throw new IllegalStateException(
                    "Token embeddings length (" + tokenEmbeddings.length + ") does not match attention mask length ("
                            + attentionMask.length + ")"
            );
        }

        if (tokenEmbeddings.length == 0) {
            throw new IllegalStateException("Model returned an empty token embedding sequence");
        }

        int embeddingDimension = tokenEmbeddings[0].length;
        float[] pooled = new float[embeddingDimension];
        int validTokenCount = 0;

        for (int tokenIndex = 0; tokenIndex < tokenEmbeddings.length; tokenIndex++) {
            if (attentionMask[tokenIndex] == 0L) {
                continue;
            }

            float[] tokenEmbedding = tokenEmbeddings[tokenIndex];

            if (tokenEmbedding.length != embeddingDimension) {
                throw new IllegalStateException(
                        "Inconsistent embedding dimension at token index " + tokenIndex
                                + ": expected " + embeddingDimension
                                + ", but got " + tokenEmbedding.length
                );
            }

            validTokenCount++;

            for (int dimension = 0; dimension < embeddingDimension; dimension++) {
                pooled[dimension] += tokenEmbedding[dimension];
            }
        }

        if (validTokenCount == 0) {
            return pooled;
        }

        for (int dimension = 0; dimension < embeddingDimension; dimension++) {
            pooled[dimension] /= validTokenCount;
        }

        return pooled;
    }

    /**
     * Returns a new L2-normalized copy of the given vector.
     *
     * <p>If the vector norm is zero, a copy of the original vector is returned.
     *
     * @param vector vector to normalize
     * @return normalized vector copy
     */
    private float[] normalizeL2(float[] vector) {
        float[] normalized = Arrays.copyOf(vector, vector.length);

        double squaredSum = 0.0d;
        for (float value : normalized) {
            squaredSum += value * value;
        }

        double norm = Math.sqrt(squaredSum);

        if (norm == 0.0d) {
            return normalized;
        }

        for (int i = 0; i < normalized.length; i++) {
            normalized[i] /= (float) norm;
        }

        return normalized;
    }

    /**
     * Truncates an array to the given maximum length.
     *
     * @param values source array
     * @return original array if already within the limit; otherwise a truncated copy
     */
    private long[] truncate(long[] values) {
        if (values == null) {
            return new long[0];
        }

        if (values.length <= OnnxEmbeddingProvider.MAX_SEQUENCE_LENGTH) {
            return values;
        }

        return Arrays.copyOf(values, OnnxEmbeddingProvider.MAX_SEQUENCE_LENGTH);
    }

    /**
     * Validates user input before tokenization.
     *
     * @param text input text
     * @throws IllegalArgumentException if text is null or blank
     */
    private void validateInput(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Input text cannot be null or blank");
        }
    }

    /**
     * Validates the shape and structural consistency of the ONNX model output.
     *
     * @param output raw model output
     * @throws IllegalStateException if the output is empty or structurally invalid
     */
    private void validateModelOutput(float[][][] output) {
        if (output.length != 1) {
            throw new IllegalStateException(
                    "Expected batch size 1, but model returned batch size " + output.length
            );
        }

        if (output[0].length == 0) {
            throw new IllegalStateException("Model returned zero token embeddings");
        }

        int embeddingDimension = output[0][0].length;
        if (embeddingDimension == 0) {
            throw new IllegalStateException("Model returned zero-dimensional embeddings");
        }

        for (int tokenIndex = 0; tokenIndex < output[0].length; tokenIndex++) {
            if (output[0][tokenIndex].length != embeddingDimension) {
                throw new IllegalStateException(
                        "Inconsistent embedding dimension at output token index " + tokenIndex
                                + ": expected " + embeddingDimension
                                + ", but got " + output[0][tokenIndex].length
                );
            }
        }
    }

    /**
     * Checks whether the loaded ONNX model declares a given input name.
     *
     * @return {@code true} if the session expects that input
     */
    private boolean expectsInput() throws OrtException {
        Map<String, NodeInfo> inputInfo = session.getInputInfo();
        return inputInfo.containsKey(OnnxEmbeddingProvider.TOKEN_TYPE_IDS_NAME);
    }

    /**
     * Creates a default token type id array filled with zeros.
     *
     * <p>This is commonly valid for single-sequence transformer inputs.
     *
     * @param length desired array length
     * @return zero-filled token type id array
     */
    private long[] createDefaultTokenTypeIds(int length) {
        return new long[length];
    }
}