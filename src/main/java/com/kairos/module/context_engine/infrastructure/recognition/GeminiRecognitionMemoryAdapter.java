package com.kairos.module.context_engine.infrastructure.recognition;

import com.kairos.module.context_engine.domain.model.retrieval.candidate.TripleCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.seed.ConceptSeedTarget;
import com.kairos.module.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.module.context_engine.domain.port.recognition.RecognitionMemory;
import com.kairos.module.context_engine.infrastructure.recognition.dto.RecognizedConcept;
import com.kairos.module.context_engine.infrastructure.recognition.dto.RecognitionMemoryResult;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class GeminiRecognitionMemoryAdapter implements RecognitionMemory {

    static final String PROMPT = """
            You are the recognition memory stage of a graph retrieval pipeline.

            Given a user question and a ranked list of candidate triples, select the concepts that should seed graph
            Personalized PageRank. Only choose concepts that are directly useful for answering the question.

            Rules:
            - Return at most {maxSeeds} concepts.
            - Every returned concept must be exactly one candidate triple subject or object.
            - Do not return predicates.
            - Do not invent, translate, rename, pluralize, or normalize concepts beyond the supplied subject/object text.
            - Prefer a smaller set of highly relevant concepts over broad recall.
            - Use confidence from 0.0 to 1.0, where higher means the concept is more central to the question.

            Question:
            {question}

            Candidate triples:
            {triples}
            """;

    private final ChatClient chatClient;

    public GeminiRecognitionMemoryAdapter(@Qualifier("recognitionMemoryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public List<GraphSeed> recognize(String searchTerm, List<TripleCandidate> candidates, int maxSeeds) {
        if (searchTerm == null || searchTerm.isBlank() || candidates == null || candidates.isEmpty() || maxSeeds <= 0) {
            return List.of();
        }

        RecognitionMemoryResult result = chatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .user(user -> user.text(PROMPT)
                        .param("question", searchTerm)
                        .param("triples", renderCandidates(candidates))
                        .param("maxSeeds", Integer.toString(maxSeeds)))
                .call()
                .entity(RecognitionMemoryResult.class);

        return toSeeds(result, candidates, maxSeeds);
    }

    public List<GraphSeed> toSeeds(RecognitionMemoryResult result, List<TripleCandidate> candidates, int maxSeeds) {
        if (result == null || result.concepts() == null || candidates == null || candidates.isEmpty() || maxSeeds <= 0) {
            return List.of();
        }

        Map<String, AllowedConcepts> allowedConceptsByTripleKey = new LinkedHashMap<>();
        for (TripleCandidate candidate : candidates) {
            allowedConceptsByTripleKey.put(candidate.key(), AllowedConcepts.from(candidate));
        }

        Map<String, GraphSeed> seedsByConcept = new LinkedHashMap<>();
        for (RecognizedConcept recognized : result.concepts()) {
            if (seedsByConcept.size() >= maxSeeds) {
                break;
            }

            GraphSeed seed = toSeed(recognized, allowedConceptsByTripleKey);
            if (seed == null) {
                continue;
            }

            String dedupeKey = ((ConceptSeedTarget) seed.target())
                    .concept()
                    .name()
                    .toLowerCase(Locale.ROOT);
            seedsByConcept.putIfAbsent(dedupeKey, seed);
        }

        return List.copyOf(seedsByConcept.values());
    }

    private GraphSeed toSeed(RecognizedConcept recognized, Map<String, AllowedConcepts> allowedConceptsByTripleKey) {
        if (recognized == null
                || recognized.tripleKey() == null
                || recognized.concept() == null
                || recognized.concept().isBlank()
                || !Double.isFinite(recognized.confidence())
                || recognized.confidence() <= 0
                || recognized.confidence() > 1) {
            return null;
        }

        AllowedConcepts allowedConcepts = allowedConceptsByTripleKey.get(recognized.tripleKey());
        if (allowedConcepts == null) {
            return null;
        }

        String canonicalConcept = allowedConcepts.canonicalConcept(recognized.concept());
        if (canonicalConcept == null) {
            return null;
        }

        return GraphSeed.concept(canonicalConcept, recognized.confidence());
    }

    private String renderCandidates(List<TripleCandidate> candidates) {
        return candidates.stream()
                .filter(Objects::nonNull)
                .map(candidate -> "- key: %s | subject: %s | predicate: %s | object: %s | score: %.6f".formatted(
                        candidate.key(),
                        candidate.subject(),
                        candidate.predicate(),
                        candidate.object(),
                        candidate.similarityScore()
                ))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private record AllowedConcepts(String subject, String object) {

        private static AllowedConcepts from(TripleCandidate candidate) {
            return new AllowedConcepts(candidate.subject(), candidate.object());
        }

        private String canonicalConcept(String concept) {
            String normalized = concept.trim().toLowerCase(Locale.ROOT);
            if (subject.toLowerCase(Locale.ROOT).equals(normalized)) {
                return subject;
            }
            if (object.toLowerCase(Locale.ROOT).equals(normalized)) {
                return object;
            }
            return null;
        }
    }
}
