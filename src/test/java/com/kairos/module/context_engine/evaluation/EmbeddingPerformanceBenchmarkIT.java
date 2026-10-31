package com.kairos.module.context_engine.evaluation;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.module.context_engine.infrastructure.embedding.onnx.OnnxEmbeddingProvider;
import com.kairos.module.context_engine.infrastructure.embedding.onnx.factory.OrtTensorFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in local ONNX measurement, not a normal CI performance assertion.
 * Invoke with -Dtest=EmbeddingPerformanceBenchmarkIT and -Dmetrics.commitSha=<SHA>.
 */
@Tag("performance-benchmark")
class EmbeddingPerformanceBenchmarkIT {
    private static final int WARMUP = 20;
    private static final int SAMPLES = 100;

    @Test
    void measuresRealJVMEmbeddingProviderWithoutExternalApi() throws Exception {
        Path model = unpack("model/model.onnx", ".onnx");
        Path tokenizerFile = unpack("model/tokenizer.json", ".json");
        Path output = Path.of(System.getProperty("metrics.outputDir", "target/metrics"));
        Files.createDirectories(output);

        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            try (OrtSession session = env.createSession(model.toString(), options)) {
                HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile);
                OnnxEmbeddingProvider provider = new OnnxEmbeddingProvider(env, session, tokenizer, new OrtTensorFactory());
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("metric_id", "M12");
                result.put("commit_sha", System.getProperty("metrics.commitSha", "unknown"));
                result.put("timestamp_utc", Instant.now().toString());
                result.put("java_version", System.getProperty("java.version"));
                result.put("os", System.getProperty("os.name"));
                result.put("available_processors", Runtime.getRuntime().availableProcessors());
                result.put("warmup_per_size", WARMUP);
                result.put("measured_per_size", SAMPLES);
                result.put("scope", "ONNX/tokenizer/mean-pool/normalization; no PostgreSQL or network");
                List<Map<String, Object>> series = new ArrayList<>();
                for (int words : new int[]{32, 128, 256}) {
                    String input = ("Distributed systems require durable data and reliable consumers. ")
                            .repeat((words + 7) / 8);
                    for (int i = 0; i < WARMUP; i++) {
                        assertThat(provider.embed(input)).hasSize(384);
                    }
                    List<Double> milliseconds = new ArrayList<>();
                    long start = System.nanoTime();
                    for (int i = 0; i < SAMPLES; i++) {
                        long t0 = System.nanoTime();
                        float[] vector = provider.embed(input);
                        long elapsed = System.nanoTime() - t0;
                        assertThat(vector).hasSize(384);
                        milliseconds.add(elapsed / 1_000_000.0);
                    }
                    double durationSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
                    double[] ordered = milliseconds.stream().mapToDouble(Double::doubleValue).sorted().toArray();
                    Map<String, Object> measurement = new LinkedHashMap<>();
                    measurement.put("approximate_input_words", words);
                    measurement.put("samples_ms", milliseconds);
                    measurement.put("p50_ms", percentile(ordered, 0.50));
                    measurement.put("p95_ms", percentile(ordered, 0.95));
                    measurement.put("p99_ms", percentile(ordered, 0.99));
                    measurement.put("embeddings_per_second", SAMPLES / durationSeconds);
                    series.add(measurement);
                }
                result.put("series", series);
                result.put("limitations", "Warm JVM only; synthetic English text; words are not tokenizer token counts; independent process measurements are required for cold start and memory peaks.");
                new ObjectMapper().writerWithDefaultPrettyPrinter()
                        .writeValue(output.resolve("embedding-performance.json").toFile(), result);
            }
        } finally {
            Files.deleteIfExists(model);
            Files.deleteIfExists(tokenizerFile);
        }
        assertThat(output.resolve("embedding-performance.json")).exists();
    }

    private static Path unpack(String resource, String suffix) throws Exception {
        try (InputStream in = EmbeddingPerformanceBenchmarkIT.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing required model resource: " + resource);
            }
            Path file = Files.createTempFile("kairos-benchmark-", suffix);
            Files.copy(in, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return file;
        }
    }

    private static double percentile(double[] sorted, double percentile) {
        if (sorted.length == 0) {
            throw new IllegalArgumentException("No successful measurements");
        }
        return sorted[Math.max(0, Math.min(sorted.length - 1,
                (int) Math.ceil(percentile * sorted.length) - 1))];
    }
}
