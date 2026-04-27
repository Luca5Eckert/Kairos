package com.kairos.context_engine.infrastructure.embedding.onnx;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.kairos.context_engine.domain.exception.EmbeddingException;
import com.kairos.context_engine.infrastructure.embedding.onnx.OnnxEmbeddingProvider;
import com.kairos.context_engine.infrastructure.embedding.onnx.factory.TensorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnnxEmbeddingProvider}.
 *
 * <p>ORT infrastructure (environment, session, tokenizer) is mocked so no
 * native libraries or model files are required on the test classpath.
 * {@link TensorFactory} is also mocked, which is the key change that
 * prevents {@code OnnxTensor.createTensor} from touching the native runtime.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OnnxEmbeddingProvider")
class OnnxEmbeddingProviderTest {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final int    EMBEDDING_DIM    = 4;
    private static final String SAMPLE_TEXT      = "Kairos knowledge graph";
    private static final long[] DEFAULT_IDS      = { 101, 2023L, 102L };
    private static final long[] DEFAULT_MASK     = { 1,   1,    1   };
    private static final long[] DEFAULT_TYPE_IDS = { 0,   0,    0   };

    // -------------------------------------------------------------------------
    // Mocks
    // -------------------------------------------------------------------------

    @Mock private OrtEnvironment        environment;
    @Mock private OrtSession            session;
    @Mock private HuggingFaceTokenizer  tokenizer;
    @Mock private TensorFactory         tensorFactory;
    @Mock private Encoding              encoding;

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    /** Provider whose model does NOT declare token_type_ids. */
    private OnnxEmbeddingProvider providerWithoutTokenTypeIds() throws OrtException {
        when(session.getInputInfo()).thenReturn(Map.of());
        return new OnnxEmbeddingProvider(environment, session, tokenizer, tensorFactory);
    }

    /** Provider whose model DOES declare token_type_ids. */
    private OnnxEmbeddingProvider providerWithTokenTypeIds() throws OrtException {
        NodeInfo nodeInfo = mock(NodeInfo.class);
        when(session.getInputInfo()).thenReturn(Map.of("token_type_ids", nodeInfo));
        return new OnnxEmbeddingProvider(environment, session, tokenizer, tensorFactory);
    }

    /**
     * Stubs the tokenizer to return the given arrays for any encode call.
     */
    private void stubTokenizer(long[] ids, long[] mask, long[] typeIds) {
        when(tokenizer.encode(anyString())).thenReturn(encoding);
        when(encoding.getIds()).thenReturn(ids);
        when(encoding.getAttentionMask()).thenReturn(mask);
        when(encoding.getTypeIds()).thenReturn(typeIds);
    }

    /**
     * Stubs the tensor factory to return a fresh mock tensor for every call,
     * then stubs the session to return the given output tensor contents.
     */
    private void stubInference(float[][][] output) throws OrtException {
        OnnxTensor        dummyTensor = mock(OnnxTensor.class);
        OrtSession.Result result      = mock(OrtSession.Result.class);
        OnnxTensor        outTensor   = mock(OnnxTensor.class);

        when(tensorFactory.createLongTensor(any(), any())).thenReturn(dummyTensor);
        when(session.run(any())).thenReturn(result);
        when(result.size()).thenReturn(1);
        when(result.get(0)).thenReturn(outTensor);
        when(outTensor.getValue()).thenReturn(output);
    }

    /** Convenience overload that builds a predictable output for the given sequence length. */
    private void stubInference(int sequenceLength) throws OrtException {
        stubInference(buildOutput(sequenceLength, EMBEDDING_DIM));
    }

    /**
     * Builds a {@code [1][sequenceLen][dim]} tensor with deterministic values:
     * {@code output[0][t][d] = (t + 1) * (d + 1) * 0.1f}.
     */
    private static float[][][] buildOutput(int sequenceLen, int dim) {
        float[][][] out = new float[1][sequenceLen][dim];
        for (int t = 0; t < sequenceLen; t++)
            for (int d = 0; d < dim; d++)
                out[0][t][d] = (t + 1) * (d + 1) * 0.1f;
        return out;
    }

    // =========================================================================
    // Construction
    // =========================================================================

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("throws NullPointerException when environment is null")
        void nullEnvironment() {
            assertThatThrownBy(() -> new OnnxEmbeddingProvider(null, session, tokenizer, tensorFactory))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("environment cannot be null");
        }

        @Test
        @DisplayName("throws NullPointerException when session is null")
        void nullSession() {
            assertThatThrownBy(() -> new OnnxEmbeddingProvider(environment, null, tokenizer, tensorFactory))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("session cannot be null");
        }

        @Test
        @DisplayName("throws NullPointerException when tokenizer is null")
        void nullTokenizer() {
            assertThatThrownBy(() -> new OnnxEmbeddingProvider(environment, session, null, tensorFactory))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("tokenizer cannot be null");
        }

        @Test
        @DisplayName("throws NullPointerException when tensorFactory is null")
        void nullTensorFactory() {
            assertThatThrownBy(() -> new OnnxEmbeddingProvider(environment, session, tokenizer, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("tensorFactory cannot be null");
        }

        @Test
        @DisplayName("wraps OrtException from session metadata into EmbeddingException")
        void metadataReadFailure() throws OrtException {
            when(session.getInputInfo()).thenThrow(new OrtException("metadata unavailable"));

            assertThatThrownBy(() -> new OnnxEmbeddingProvider(environment, session, tokenizer, tensorFactory))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("Failed to read ONNX session input metadata");
        }

        @Test
        @DisplayName("resolves token_type_ids support once — does not re-query session on embed()")
        void tokenTypeIdsResolvedOnce() throws OrtException {
            when(session.getInputInfo()).thenReturn(Map.of());
            OnnxEmbeddingProvider provider = new OnnxEmbeddingProvider(environment, session, tokenizer, tensorFactory);

            stubTokenizer(DEFAULT_IDS, DEFAULT_MASK, DEFAULT_TYPE_IDS);
            stubInference(DEFAULT_IDS.length);

            provider.embed(SAMPLE_TEXT);

            // getInputInfo() must have been called exactly once — during construction.
            verify(session).getInputInfo();
        }
    }

    // =========================================================================
    // Input validation
    // =========================================================================

    @Nested
    @DisplayName("input validation")
    class InputValidation {

        private OnnxEmbeddingProvider provider;

        @BeforeEach
        void setUp() throws OrtException {
            provider = providerWithoutTokenTypeIds();
        }

        @Test
        @DisplayName("throws IllegalArgumentException when text is null")
        void nullText() {
            assertThatThrownBy(() -> provider.embed(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when text is blank")
        void blankText() {
            assertThatThrownBy(() -> provider.embed("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when text is empty")
        void emptyText() {
            assertThatThrownBy(() -> provider.embed(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Nested
    @DisplayName("embed — happy path")
    class EmbedHappyPath {

        @Test
        @DisplayName("returns a float[] of the model's embedding dimension")
        void returnsCorrectDimension() throws OrtException {
            OnnxEmbeddingProvider provider = providerWithoutTokenTypeIds();
            stubTokenizer(DEFAULT_IDS, DEFAULT_MASK, DEFAULT_TYPE_IDS);
            stubInference(DEFAULT_IDS.length);

            assertThat(provider.embed(SAMPLE_TEXT)).hasSize(EMBEDDING_DIM);
        }

        @Test
        @DisplayName("result is L2-normalized — its norm is approximately 1.0")
        void resultIsNormalized() throws OrtException {
            OnnxEmbeddingProvider provider = providerWithoutTokenTypeIds();
            stubTokenizer(DEFAULT_IDS, DEFAULT_MASK, DEFAULT_TYPE_IDS);
            stubInference(DEFAULT_IDS.length);

            float[] result     = provider.embed(SAMPLE_TEXT);
            double  squaredSum = 0.0;
            for (float v : result) squaredSum += (double) v * v;

            assertThat(Math.sqrt(squaredSum)).isCloseTo(1.0, within(1e-5));
        }

        @Test
        @DisplayName("does not include token_type_ids in session inputs when model does not declare it")
        void doesNotSendTokenTypeIdsWhenNotDeclared() throws OrtException {
            OnnxEmbeddingProvider provider = providerWithoutTokenTypeIds();
            stubTokenizer(DEFAULT_IDS, DEFAULT_MASK, DEFAULT_TYPE_IDS);

            OnnxTensor        dummyTensor = mock(OnnxTensor.class);
            OrtSession.Result result      = mock(OrtSession.Result.class);
            OnnxTensor        outTensor   = mock(OnnxTensor.class);

            when(tensorFactory.createLongTensor(any(), any())).thenReturn(dummyTensor);
            when(session.run(any())).thenAnswer(inv -> {
                Map<String, OnnxTensor> inputs = inv.getArgument(0);
                assertThat(inputs).doesNotContainKey("token_type_ids");
                return result;
            });
            when(result.size()).thenReturn(1);
            when(result.get(0)).thenReturn(outTensor);
            when(outTensor.getValue()).thenReturn(buildOutput(DEFAULT_IDS.length, EMBEDDING_DIM));

            provider.embed(SAMPLE_TEXT);
        }

        @Test
        @DisplayName("includes token_type_ids in session inputs when model declares it")
        void sendsTokenTypeIdsWhenDeclared() throws OrtException {
            OnnxEmbeddingProvider provider = providerWithTokenTypeIds();
            stubTokenizer(DEFAULT_IDS, DEFAULT_MASK, DEFAULT_TYPE_IDS);

            OnnxTensor        dummyTensor = mock(OnnxTensor.class);
            OrtSession.Result result      = mock(OrtSession.Result.class);
            OnnxTensor        outTensor   = mock(OnnxTensor.class);

            when(tensorFactory.createLongTensor(any(), any())).thenReturn(dummyTensor);
            when(session.run(any())).thenAnswer(inv -> {
                Map<String, OnnxTensor> inputs = inv.getArgument(0);
                assertThat(inputs).containsKey("token_type_ids");
                return result;
            });
            when(result.size()).thenReturn(1);
            when(result.get(0)).thenReturn(outTensor);
            when(outTensor.getValue()).thenReturn(buildOutput(DEFAULT_IDS.length, EMBEDDING_DIM));

            provider.embed(SAMPLE_TEXT);
        }
    }

    // =========================================================================
    // Mean pooling
    // =========================================================================

    @Nested
    @DisplayName("mean pooling")
    class MeanPooling {

        @Test
        @DisplayName("masked tokens (attention = 0) are excluded from the average")
        void maskedTokensExcluded() throws OrtException {
            OnnxEmbeddingProvider provider = providerWithoutTokenTypeIds();

            stubTokenizer(
                    new long[]{ 101, 2023L, 0 },
                    new long[]{ 1,   1,    0 },
                    new long[]{ 0,   0,    0 }
            );

            // token 2 must be ignored despite its large values
            stubInference(new float[][][] {{
                    { 1f, 0f, 0f, 0f },
                    { 0f, 1f, 0f, 0f },
                    { 99f, 99f, 99f, 99f }
            }});

            float[] result = provider.embed(SAMPLE_TEXT);

            // mean of tokens 0 and 1 before normalization: [0.5, 0.5, 0, 0]
            // after L2: [1/√2, 1/√2, 0, 0]
            float expected = (float) (1.0 / Math.sqrt(2));
            assertThat(result[0]).isCloseTo(expected, within(1e-5f));
            assertThat(result[1]).isCloseTo(expected, within(1e-5f));
            assertThat(result[2]).isCloseTo(0f,       within(1e-5f));
            assertThat(result[3]).isCloseTo(0f,       within(1e-5f));
        }

        @Test
        @DisplayName("all-masked sequence returns a zero vector without throwing")
        void allMaskedReturnsZeroVector() throws OrtException {
            OnnxEmbeddingProvider provider = providerWithoutTokenTypeIds();

            stubTokenizer(
                    new long[]{ 101 },
                    new long[]{ 0   },
                    new long[]{ 0   }
            );
            stubInference(new float[][][] {{ { 9f, 9f, 9f, 9f } }});

            assertThat(provider.embed(SAMPLE_TEXT)).containsOnly(0f);
        }
    }

    // =========================================================================
    // Inference failures
    // =========================================================================

    @Nested
    @DisplayName("inference failures")
    class InferenceFailures {

        private OnnxEmbeddingProvider provider;

        @BeforeEach
        void setUp() throws OrtException {
            provider = providerWithoutTokenTypeIds();
        }

        @Test
        @DisplayName("wraps OrtException from tensor creation into EmbeddingException")
        void tensorCreationFailure() throws OrtException {
            stubTokenizer(DEFAULT_IDS, DEFAULT_MASK, DEFAULT_TYPE_IDS);
            when(tensorFactory.createLongTensor(any(), any()))
                    .thenThrow(new OrtException("GPU out of memory"));

            assertThatThrownBy(() -> provider.embed(SAMPLE_TEXT))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("Failed to run embedding inference")
                    .hasCauseInstanceOf(OrtException.class);
        }

        @Test
        @DisplayName("wraps OrtException from session.run into EmbeddingException")
        void sessionRunFailure() throws OrtException {
            stubTokenizer(DEFAULT_IDS, DEFAULT_MASK, DEFAULT_TYPE_IDS);
            when(tensorFactory.createLongTensor(any(), any())).thenReturn(mock(OnnxTensor.class));
            when(session.run(any())).thenThrow(new OrtException("inference crash"));

            assertThatThrownBy(() -> provider.embed(SAMPLE_TEXT))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("Failed to run embedding inference")
                    .hasCauseInstanceOf(OrtException.class);
        }

        @Test
        @DisplayName("throws EmbeddingException when model returns no outputs")
        void noOutputs() throws OrtException {
            stubTokenizer(DEFAULT_IDS, DEFAULT_MASK, DEFAULT_TYPE_IDS);

            OnnxTensor        dummy  = mock(OnnxTensor.class);
            OrtSession.Result result = mock(OrtSession.Result.class);

            when(tensorFactory.createLongTensor(any(), any())).thenReturn(dummy);
            when(session.run(any())).thenReturn(result);
            when(result.size()).thenReturn(0);

            assertThatThrownBy(() -> provider.embed(SAMPLE_TEXT))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("no outputs");
        }

        @Test
        @DisplayName("throws EmbeddingException when output type is not float[][][]")
        void unexpectedOutputType() throws OrtException {
            stubTokenizer(DEFAULT_IDS, DEFAULT_MASK, DEFAULT_TYPE_IDS);

            OnnxTensor        dummy     = mock(OnnxTensor.class);
            OrtSession.Result result    = mock(OrtSession.Result.class);
            OnnxTensor        outTensor = mock(OnnxTensor.class);

            when(tensorFactory.createLongTensor(any(), any())).thenReturn(dummy);
            when(session.run(any())).thenReturn(result);
            when(result.size()).thenReturn(1);
            when(result.get(0)).thenReturn(outTensor);
            when(outTensor.getValue()).thenReturn(new double[][][] {});

            assertThatThrownBy(() -> provider.embed(SAMPLE_TEXT))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("Unexpected ONNX output type");
        }

        @Test
        @DisplayName("throws EmbeddingException when batch size is not 1")
        void unexpectedBatchSize() throws OrtException {
            stubTokenizer(DEFAULT_IDS, DEFAULT_MASK, DEFAULT_TYPE_IDS);
            stubInference(new float[2][3][EMBEDDING_DIM]);

            assertThatThrownBy(() -> provider.embed(SAMPLE_TEXT))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("batch size");
        }

        @Test
        @DisplayName("throws EmbeddingException when model returns zero token embeddings")
        void zeroTokenEmbeddings() throws OrtException {
            stubTokenizer(DEFAULT_IDS, DEFAULT_MASK, DEFAULT_TYPE_IDS);
            stubInference(new float[1][0][EMBEDDING_DIM]);

            assertThatThrownBy(() -> provider.embed(SAMPLE_TEXT))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("zero token embeddings");
        }
    }

    // =========================================================================
    // Truncation
    // =========================================================================

    @Nested
    @DisplayName("truncation")
    class Truncation {

        @Test
        @DisplayName("sequences within MAX_SEQUENCE_LENGTH pass through without truncation")
        void noTruncationNeeded() throws OrtException {
            OnnxEmbeddingProvider provider = providerWithoutTokenTypeIds();

            long[] ids  = new long[256];
            long[] mask = new long[256];
            java.util.Arrays.fill(mask, 1);

            stubTokenizer(ids, mask, new long[256]);
            stubInference(256);

            assertThat(provider.embed(SAMPLE_TEXT)).hasSize(EMBEDDING_DIM);
        }

        @Test
        @DisplayName("sequences exceeding MAX_SEQUENCE_LENGTH are truncated to 256 tokens")
        void truncationApplied() throws OrtException {
            OnnxEmbeddingProvider provider = providerWithoutTokenTypeIds();

            long[] ids  = new long[512];
            long[] mask = new long[512];
            java.util.Arrays.fill(mask, 1);

            stubTokenizer(ids, mask, new long[512]);
            stubInference(256); // model only sees 256 tokens after truncation

            assertThat(provider.embed(SAMPLE_TEXT)).hasSize(EMBEDDING_DIM);
        }
    }

    // =========================================================================
    // Null type ids fallback
    // =========================================================================

    @Nested
    @DisplayName("tokenizer — null type ids fallback")
    class NullTypeIdsFallback {

        @Test
        @DisplayName("uses zero-filled token_type_ids when tokenizer returns null")
        void nullTypeIdsDefaultsToZeros() throws OrtException {
            OnnxEmbeddingProvider provider = providerWithTokenTypeIds();

            when(tokenizer.encode(anyString())).thenReturn(encoding);
            when(encoding.getIds()).thenReturn(DEFAULT_IDS);
            when(encoding.getAttentionMask()).thenReturn(DEFAULT_MASK);
            when(encoding.getTypeIds()).thenReturn(null); // tokenizer gives us nothing

            OnnxTensor        dummy     = mock(OnnxTensor.class);
            OrtSession.Result result    = mock(OrtSession.Result.class);
            OnnxTensor        outTensor = mock(OnnxTensor.class);

            when(tensorFactory.createLongTensor(any(), any())).thenReturn(dummy);
            when(session.run(any())).thenAnswer(inv -> {
                Map<String, OnnxTensor> inputs = inv.getArgument(0);
                // model expects token_type_ids — provider must have synthesized them
                assertThat(inputs).containsKey("token_type_ids");
                return result;
            });
            when(result.size()).thenReturn(1);
            when(result.get(0)).thenReturn(outTensor);
            when(outTensor.getValue()).thenReturn(buildOutput(DEFAULT_IDS.length, EMBEDDING_DIM));

            assertThat(provider.embed(SAMPLE_TEXT)).hasSize(EMBEDDING_DIM);
        }
    }
}