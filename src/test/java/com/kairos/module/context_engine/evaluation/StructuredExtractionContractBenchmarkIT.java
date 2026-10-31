package com.kairos.module.context_engine.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.module.context_engine.infrastructure.ai.gemini.dto.ExtractedTriple;
import com.kairos.module.context_engine.infrastructure.ai.gemini.dto.TripleExtractionResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A deterministic contract probe. It does not invoke Gemini and does not claim
 * provider latency, token counts, factual accuracy, or production schema enforcement.
 */
@Tag("performance-benchmark")
class StructuredExtractionContractBenchmarkIT {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void evaluatesStructuredExtractionPayloadsAndWritesAuditableContractReport() throws Exception {
        record Case(String id, String payload, boolean expectedValid) {}
        List<Case> corpus = List.of(
                new Case("valid", "{\"triples\":[{\"subject\":\"spring ai\",\"predicate\":\"USES\",\"object\":\"gemini\",\"weight\":0.8}]}", true),
                new Case("empty", "{\"triples\":[]}", true),
                new Case("bad-json", "{\"triples\":[", false),
                new Case("missing-subject", "{\"triples\":[{\"predicate\":\"USES\",\"object\":\"gemini\",\"weight\":0.8}]}", false),
                new Case("negative-weight", "{\"triples\":[{\"subject\":\"a\",\"predicate\":\"USES\",\"object\":\"b\",\"weight\":-0.2}]}", false),
                new Case("above-one", "{\"triples\":[{\"subject\":\"a\",\"predicate\":\"USES\",\"object\":\"b\",\"weight\":1.2}]}", false),
                new Case("wrong-predicate-case", "{\"triples\":[{\"subject\":\"a\",\"predicate\":\"uses\",\"object\":\"b\",\"weight\":0.8}]}", false)
        );
        int correct = 0;
        List<Map<String, Object>> observations = new ArrayList<>();
        for (Case fixture : corpus) {
            boolean accepted;
            try {
                TripleExtractionResult payload = mapper.readValue(fixture.payload(), TripleExtractionResult.class);
                accepted = payload.triples().stream().allMatch(StructuredExtractionContractBenchmarkIT::valid);
            } catch (Exception ignored) {
                accepted = false;
            }
            if (fixture.expectedValid() == accepted) {
                correct++;
            }
            observations.add(Map.of("case", fixture.id(), "expected_valid", fixture.expectedValid(),
                    "observed_valid", accepted, "matches_expectation", fixture.expectedValid() == accepted));
        }
        assertThat(correct).isEqualTo(corpus.size());
        Path output = Path.of(System.getProperty("metrics.outputDir", "target/metrics"));
        Files.createDirectories(output);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("metric_id", "M13");
        report.put("commit_sha", System.getProperty("metrics.commitSha", "unknown"));
        report.put("timestamp_utc", Instant.now().toString());
        report.put("total_fixtures", corpus.size());
        report.put("correct_classifications", correct);
        report.put("provider_requests", 0);
        report.put("cases", observations);
        report.put("limitations", "Test-local validator only; not enforced by GeminiTripleExtractorAdapter; no live provider, factual-accuracy, retry, latency or token-cost evidence.");
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("structured-extraction-contract.json").toFile(), report);
        assertThat(output.resolve("structured-extraction-contract.json")).exists();
    }

    private static boolean valid(ExtractedTriple triple) {
        return triple.subject() != null && !triple.subject().isBlank()
                && triple.object() != null && !triple.object().isBlank()
                && triple.predicate() != null && triple.predicate().matches("[A-Z][A-Z_]*")
                && triple.weight() != null && Double.isFinite(triple.weight())
                && triple.weight() >= 0.0 && triple.weight() <= 1.0;
    }
}
