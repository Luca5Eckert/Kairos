package com.kairos.module.context_engine.infrastructure.recognition;

import com.kairos.module.context_engine.domain.model.retrieval.candidate.TripleCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.seed.ConceptSeedTarget;
import com.kairos.module.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.module.context_engine.infrastructure.recognition.GeminiRecognitionMemoryAdapter;
import com.kairos.module.context_engine.infrastructure.recognition.dto.RecognizedConcept;
import com.kairos.module.context_engine.infrastructure.recognition.dto.RecognitionMemoryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeminiRecognitionMemoryAdapter")
class GeminiRecognitionMemoryAdapterTest {

    @Test
    @DisplayName("prompt renders with question, triples and max seed parameters")
    void promptShouldRenderWithParameters() throws Exception {
        Field promptField = GeminiRecognitionMemoryAdapter.class.getDeclaredField("PROMPT");
        promptField.setAccessible(true);

        String prompt = (String) promptField.get(null);
        String renderedPrompt = new PromptTemplate(prompt).render(Map.of(
                "question", "How does Spring Data use repositories?",
                "triples", "spring data | USES | repository pattern",
                "maxSeeds", "10"
        ));

        assertThat(renderedPrompt)
                .contains("How does Spring Data use repositories?")
                .contains("spring data | USES | repository pattern")
                .contains("10");
    }

    @Test
    @DisplayName("toSeeds keeps only accepted subject/object concepts and deduplicates canonically")
    void toSeedsValidatesRecognizedConcepts() {
        TripleCandidate candidate = tripleCandidate(
                "mind-RELATES_TO-consciousness",
                "mind",
                "RELATES_TO",
                "consciousness",
                0.91
        );
        RecognitionMemoryResult result = new RecognitionMemoryResult(List.of(
                new RecognizedConcept("mind-RELATES_TO-consciousness", "mind", 0.8),
                new RecognizedConcept("mind-RELATES_TO-consciousness", "Mind", 0.7),
                new RecognizedConcept("mind-RELATES_TO-consciousness", "consciousness", 0.6),
                new RecognizedConcept("mind-RELATES_TO-consciousness", "hallucinated", 0.9),
                new RecognizedConcept("unknown", "mind", 0.9),
                new RecognizedConcept("mind-RELATES_TO-consciousness", " ", 0.9),
                new RecognizedConcept("mind-RELATES_TO-consciousness", "mind", 0.0),
                new RecognizedConcept("mind-RELATES_TO-consciousness", "mind", 1.1)
        ));

        List<GraphSeed> seeds = new GeminiRecognitionMemoryAdapter(null)
                .toSeeds(result, List.of(candidate), 10);

        assertThat(seeds).hasSize(2);
        assertThat(seeds)
                .extracting(seed -> ((ConceptSeedTarget) seed.target()).concept().name())
                .containsExactly("mind", "consciousness");
        assertThat(seeds)
                .extracting(GraphSeed::weight)
                .containsExactly(0.8, 0.6);
    }

    @Test
    @DisplayName("toSeeds respects the configured maximum seed count")
    void toSeedsRespectsMaxSeedCount() {
        List<TripleCandidate> candidates = IntStream.range(0, 12)
                .mapToObj(index -> tripleCandidate(
                        "subject-" + index + "-REL-object-" + index,
                        "subject-" + index,
                        "REL",
                        "object-" + index,
                        0.9
                ))
                .toList();
        RecognitionMemoryResult result = new RecognitionMemoryResult(
                candidates.stream()
                        .map(candidate -> new RecognizedConcept(candidate.key(), candidate.subject(), 0.8))
                        .toList()
        );

        List<GraphSeed> seeds = new GeminiRecognitionMemoryAdapter(null)
                .toSeeds(result, candidates, 10);

        assertThat(seeds).hasSize(10);
        assertThat(seeds)
                .extracting(seed -> ((ConceptSeedTarget) seed.target()).concept().name())
                .containsExactly(
                        "subject-0", "subject-1", "subject-2", "subject-3", "subject-4",
                        "subject-5", "subject-6", "subject-7", "subject-8", "subject-9"
                );
    }

    @Test
    @DisplayName("toSeeds returns empty when model returns no structured concepts")
    void toSeedsReturnsEmptyForMissingResult() {
        List<GraphSeed> seeds = new GeminiRecognitionMemoryAdapter(null)
                .toSeeds(new RecognitionMemoryResult(null), List.of(tripleCandidate(
                        "mind-RELATES_TO-consciousness",
                        "mind",
                        "RELATES_TO",
                        "consciousness",
                        0.91
                )), 10);

        assertThat(seeds).isEmpty();
    }

    private TripleCandidate tripleCandidate(
            String key,
            String subject,
            String predicate,
            String object,
            double similarity
    ) {
        return new TripleCandidate(key, subject, predicate, object, UUID.randomUUID(), similarity);
    }
}
