package com.kairos.infrastructure.embedding;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.kairos.domain.embedding.EmbeddingProvider;
import com.kairos.domain.exception.EmbeddingException;
import com.kairos.infrastructure.embedding.factory.TensorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 */
@Component
public class OnnxEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OnnxEmbeddingProvider.class);

    /**
     * Maximum number of tokens sent to the model.
     *
     * <p>all-MiniLM-L6-v2 supports up to 512 tokens. This limit is set conservatively
     * to 256 to account for the sliding-window chunking strategy defined in ADR-005
     * (200-token chunks with 40-token overlap). Sequences exceeding this limit are
     * truncated with a warning, since truncation silently degrades embedding quality.
     */
    private static final int MAX_SEQUENCE_LENGTH = 256;

    private static final String INPUT_IDS_NAME      = "input_ids";
    private static final String ATTENTION_MASK_NAME = "attention_mask";
    private static final String TOKEN_TYPE_IDS_NAME = "token_type_ids";

    private final OrtEnvironment       environment;
    private final OrtSession           session;
    private final HuggingFaceTokenizer tokenizer;
    private final TensorFactory        tensorFactory;

    /**
     * Whether the loaded ONNX model declares {@code token_type_ids} as an input.
     *
     * <p>Resolved once at construction time so every call to {@link #embed} avoids
     * the overhead of querying session metadata.
     */
    private final boolean modelExpectsTokenTypeIds;

    /**
     * Creates a new provider.
     *
     * @param environment   ONNX Runtime environment
     * @param session       ONNX Runtime session containing the embedding model
     * @param tokenizer     tokenizer compatible with the ONNX model
     * @param tensorFactory factory used to create ORT input tensors
     * @throws EmbeddingException if session metadata cannot be read during initialization
     */
    public OnnxEmbeddingProvider(
            OrtEnvironment environment,
            OrtSession session,
            HuggingFaceTokenizer tokenizer,
            TensorFactory tensorFactory
    ) {
        this.environment   = Objects.requireNonNull(environment,   "environment cannot be null");
        this.session       = Objects.requireNonNull(session,       "session cannot be null");
        this.tokenizer     = Objects.requireNonNull(tokenizer,     "tokenizer cannot be null");
        this.tensorFactory = Objects.requireNonNull(tensorFactory, "tensorFactory cannot be null");

        this.modelExpectsTokenTypeIds = resolveTokenTypeIdsSupport(session);

        log.info(
                "OnnxEmbeddingProvider initialized — token_type_ids: {}",
                modelExpectsTokenTypeIds ? "required" : "not declared"
        );
    }

    /**
     * Generates a normalized embedding vector for the given text.
     *
     * @param text input text to embed
     * @return normalized embedding vector
     * @throws IllegalArgumentException if the input text is null or blank
     * @throws EmbeddingException       if model inference fails or the model output is invalid
     */
    @Override
    public float[] embed(String text) {
        validateInput(text);

        TokenizedInput tokenizedInput  = tokenize(text);
        float[][]      tokenEmbeddings = infer(tokenizedInput);
        float[]        pooledEmbedding = meanPool(tokenEmbeddings, tokenizedInput.attentionMask());

        return normalizeL2(pooledEmbedding);
    }

    private TokenizedInput tokenize(String text) {
        Encoding encoding = tokenizer.encode(text);

        long[] inputIds      = truncate(encoding.getIds());
        long[] attentionMask = truncate(encoding.getAttentionMask());
        long[] tokenTypeIds  = encoding.getTypeIds() != null
                ? truncate(encoding.getTypeIds())
                : createDefaultTokenTypeIds(inputIds.length);

        return new TokenizedInput(inputIds, attentionMask, tokenTypeIds);
    }

    /**
     * Runs ONNX inference and returns token-level embeddings.
     *
     * <p>Expected output shape:
     * <pre>[batch_size=1, sequence_length, embedding_dimension]</pre>
     *
     * @throws EmbeddingException if inference fails or the output shape is invalid
     */
    private float[][] infer(TokenizedInput tokenizedInput) {
        long[][] inputIds      = { tokenizedInput.inputIds() };
        long[][] attentionMask = { tokenizedInput.attentionMask() };
        long[][] tokenTypeIds  = { tokenizedInput.tokenTypeIds() };

        try (
                OnnxTensor inputIdsTensor      = tensorFactory.createLongTensor(environment, inputIds);
                OnnxTensor attentionMaskTensor = tensorFactory.createLongTensor(environment, attentionMask)
        ) {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put(INPUT_IDS_NAME,      inputIdsTensor);
            inputs.put(ATTENTION_MASK_NAME, attentionMaskTensor);

            if (modelExpectsTokenTypeIds) {
                try (OnnxTensor tokenTypeIdsTensor = tensorFactory.createLongTensor(environment, tokenTypeIds)) {
                    inputs.put(TOKEN_TYPE_IDS_NAME, tokenTypeIdsTensor);
                }
            }

            try (OrtSession.Result result = session.run(inputs)) {
                if (result.size() == 0) {
                    throw new EmbeddingException("ONNX model returned no outputs");
                }

                Object rawOutput = result.get(0).getValue();

                if (!(rawOutput instanceof float[][][] output)) {
                    throw new EmbeddingException(
                            "Unexpected ONNX output type: expected float[][][], got "
                                    + (rawOutput == null ? "null" : rawOutput.getClass().getName())
                    );
                }

                validateModelOutput(output);
                return output[0];
            }

        } catch (OrtException e) {
            throw new EmbeddingException("Failed to run embedding inference", e);
        }
    }

    private float[] meanPool(float[][] tokenEmbeddings, long[] attentionMask) {
        if (tokenEmbeddings.length == 0) {
            throw new EmbeddingException("Model returned an empty token embedding sequence");
        }

        if (tokenEmbeddings.length != attentionMask.length) {
            throw new EmbeddingException(
                    "Token embeddings length (" + tokenEmbeddings.length
                            + ") does not match attention mask length (" + attentionMask.length + ")"
            );
        }

        int     embeddingDimension = tokenEmbeddings[0].length;
        float[] pooled             = new float[embeddingDimension];
        int     validTokenCount    = 0;

        for (int i = 0; i < tokenEmbeddings.length; i++) {
            if (attentionMask[i] == 0L) continue;

            float[] token = tokenEmbeddings[i];

            if (token.length != embeddingDimension) {
                throw new EmbeddingException(
                        "Inconsistent embedding dimension at token index " + i
                                + ": expected " + embeddingDimension + ", got " + token.length
                );
            }

            validTokenCount++;
            for (int d = 0; d < embeddingDimension; d++) pooled[d] += token[d];
        }

        if (validTokenCount == 0) return pooled;

        for (int d = 0; d < embeddingDimension; d++) pooled[d] /= validTokenCount;

        return pooled;
    }

    private float[] normalizeL2(float[] vector) {
        float[]  normalized = Arrays.copyOf(vector, vector.length);
        double   squaredSum = 0.0d;

        for (float v : normalized) squaredSum += (double) v * v;

        double norm = Math.sqrt(squaredSum);
        if (norm == 0.0d) return normalized;

        for (int i = 0; i < normalized.length; i++) normalized[i] /= (float) norm;

        return normalized;
    }

    private void validateInput(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Input text cannot be null or blank");
        }
    }

    private void validateModelOutput(float[][][] output) {
        if (output.length != 1) {
            throw new EmbeddingException(
                    "Expected batch size 1, but model returned batch size " + output.length
            );
        }
        if (output[0].length == 0) {
            throw new EmbeddingException("Model returned zero token embeddings");
        }
        if (output[0][0].length == 0) {
            throw new EmbeddingException("Model returned zero-dimensional embeddings");
        }
    }

    private long[] truncate(long[] values) {
        if (values == null) return new long[0];

        if (values.length <= MAX_SEQUENCE_LENGTH) return values;

        log.warn(
                "Token sequence length {} exceeds MAX_SEQUENCE_LENGTH {}; truncating. "
                        + "This may degrade embedding quality — review your chunking strategy.",
                values.length, MAX_SEQUENCE_LENGTH
        );

        return Arrays.copyOf(values, MAX_SEQUENCE_LENGTH);
    }

    private long[] createDefaultTokenTypeIds(int length) {
        return new long[length];
    }

    private static boolean resolveTokenTypeIdsSupport(OrtSession session) {
        try {
            return session.getInputInfo().containsKey(TOKEN_TYPE_IDS_NAME);
        } catch (OrtException e) {
            throw new EmbeddingException(
                    "Failed to read ONNX session input metadata during initialization", e
            );
        }
    }
}