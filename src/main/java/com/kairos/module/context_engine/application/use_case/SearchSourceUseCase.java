package com.kairos.module.context_engine.application.use_case;

import com.kairos.module.context_engine.application.query.SearchSourceQuery;
import com.kairos.module.context_engine.domain.model.SearchResult;
import com.kairos.module.context_engine.domain.model.history.Answer;
import com.kairos.module.context_engine.domain.model.history.AnswerSnapshot;
import com.kairos.module.context_engine.domain.model.history.Question;
import com.kairos.module.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.module.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.candidate.TripleCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.module.context_engine.domain.model.retrieval.graph.GraphSearchResult;
import com.kairos.module.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.module.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.module.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.module.context_engine.domain.port.graph.KnowledgeGraphSearch;
import com.kairos.module.context_engine.domain.port.repository.HistoryRepository;
import com.kairos.module.context_engine.domain.port.recognition.RecognitionMemory;
import com.kairos.module.context_engine.domain.port.semantic.SemanticSearch;
import com.kairos.module.context_engine.infrastructure.config.RetrievalProperties;
import com.kairos.share.security.context.RequestContextProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SearchSourceUseCase {

    private final EmbeddingProvider embeddingPort;
    private final KnowledgeGraphSearch knowledgeGraphSearch;
    private final SemanticSearch semanticSearch;
    private final RecognitionMemory recognitionMemory;
    private final RequestContextProvider requestContextProvider;
    private final HistoryRepository historyRepository;
    private final RetrievalProperties retrievalProperties;

    public SearchResult execute(SearchSourceQuery query) {
        UUID userId = requestContextProvider.getRequestContext().userId();

        Question question = questionFor(query, userId);

        float[] queryVector = embeddingPort.embed(query.searchTerm());

        List<PassageCandidate> passageCandidates = semanticSearch.findPassageCandidate(
                queryVector, userId, retrievalProperties.semanticAnchorLimit());
        List<TripleCandidate> tripleCandidates = semanticSearch.findTripleCandidates(
                queryVector, userId, retrievalProperties.tripleCandidateLimit());

        List<GraphSeed> conceptSeeds = tripleCandidates.isEmpty()
                ? List.of()
                : List.copyOf(Optional.ofNullable(
                        recognitionMemory.recognize(query.searchTerm(), tripleCandidates,
                                retrievalProperties.recognitionSeedLimit())).orElse(List.of()));

        List<GraphSeed> seeds = instanceSeedsFromCandidates(passageCandidates, conceptSeeds);
        SearchResult result = retrieve(userId, seeds);

        saveAnswer(question, seeds, passageCandidates, result);

        return result;
    }

    private SearchResult retrieve(UUID userId, List<GraphSeed> seeds) {
        if (seeds.isEmpty()) {
            return SearchResult.empty();
        }

        GraphSearchResult graphResult = knowledgeGraphSearch.expandKnowledge(
                GraphSearchRequest.from(userId, seeds, retrievalProperties.graphPassageLimit())
        );
        List<RankedChunk> chunks = graphResult.scoredPassages().isEmpty()
                ? List.of()
                : semanticSearch.hydrateAndRankChunks(graphResult.scoredPassages(), userId);

        return SearchResult.from(filterActivatedTriples(graphResult.activatedTriples(), chunks), chunks);
    }

    private void saveAnswer(Question question, List<GraphSeed> seeds, List<PassageCandidate> candidates, SearchResult result) {
        Map<UUID, Double> denseScores = candidates.stream()
                .collect(Collectors.toMap(PassageCandidate::chunkId, PassageCandidate::denseScore, (first, ignored) -> first));
        AnswerSnapshot snapshot = AnswerSnapshot.from(
                parameters(), seeds, result.chunks(), denseScores, result.knowledgeTriples()
        );

        historyRepository.saveAnswer(Answer.create(question.id(), snapshot));
    }

    private AnswerSnapshot.RetrievalParameters parameters() {
        return new AnswerSnapshot.RetrievalParameters(
                retrievalProperties.semanticAnchorLimit(),
                retrievalProperties.tripleCandidateLimit(),
                retrievalProperties.recognitionSeedLimit(),
                retrievalProperties.graphPassageLimit(),
                retrievalProperties.seedMinScore(),
                retrievalProperties.seedRelativeThreshold()
        );
    }

    private Question questionFor(SearchSourceQuery query, UUID userId) {
        if (query.questionId() != null) {
            return historyRepository.findQuestionByIdAndUserId(query.questionId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException("History resource not found"));
        }

        Question question = Question.create(userId, query.searchTerm());
        historyRepository.saveQuestion(question);

        return question;
    }

    private List<GraphSeed> instanceSeedsFromCandidates(List<PassageCandidate> passageCandidates, List<GraphSeed> conceptSeeds) {
        double passageThreshold = seedThreshold(passageCandidates.stream()
                .mapToDouble(PassageCandidate::denseScore).max().orElse(0d));
        var passageSeeds = passageCandidates.stream()
                .filter(candidate -> candidate.denseScore() >= passageThreshold)
                .map(candidate -> GraphSeed.passage(candidate.chunkId(), candidate.denseScore()));

        double conceptThreshold = seedThreshold(conceptSeeds.stream()
                .mapToDouble(GraphSeed::weight).max().orElse(0d));
        var filteredConceptSeeds = conceptSeeds.stream()
                .filter(seed -> seed.weight() >= conceptThreshold);

        return Stream.concat(passageSeeds, filteredConceptSeeds).toList();
    }

    private double seedThreshold(double bestScore) {
        if (bestScore <= 0) {
            return Double.POSITIVE_INFINITY;
        }

        return Math.max(retrievalProperties.seedMinScore(),
                bestScore * retrievalProperties.seedRelativeThreshold());
    }

    private List<KnowledgeTriple> filterActivatedTriples(List<KnowledgeTriple> triples, List<RankedChunk> rankedChunks) {
        if (triples == null || triples.isEmpty() || rankedChunks == null || rankedChunks.isEmpty()) {
            return List.of();
        }

        var selectedChunkIds = rankedChunks.stream()
                .map(rankedChunk -> rankedChunk.chunk().getId())
                .collect(Collectors.toSet());

        record TripleKey(String subject, String predicate, String object) {}

        Map<TripleKey, KnowledgeTriple> triplesByKey = new LinkedHashMap<>();
        for (KnowledgeTriple triple : triples) {
            if (triple.passage() == null || !selectedChunkIds.contains(triple.passage().chunkId())) {
                continue;
            }

            TripleKey key = new TripleKey(
                    triple.subject().name(),
                    triple.predicate(),
                    triple.object().name()
            );
            triplesByKey.putIfAbsent(key, triple);
        }

        return List.copyOf(triplesByKey.values());
    }

}
