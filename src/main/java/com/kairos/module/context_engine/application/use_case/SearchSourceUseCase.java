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
import com.kairos.share.security.context.RequestContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${kairos.retrieval.semantic-anchor-limit:10}")
    private int semanticAnchorLimit = 10;

    @Value("${kairos.retrieval.graph-passage-limit:20}")
    private int graphPassageLimit = 20;

    @Value("${kairos.retrieval.triple-candidate-limit:30}")
    private int tripleCandidateLimit = 30;

    @Value("${kairos.retrieval.recognition-seed-limit:10}")
    private int recognitionSeedLimit = 10;

    @Value("${kairos.retrieval.seed-min-score:0.45}")
    private double seedMinScore = 0.45d;

    @Value("${kairos.retrieval.seed-relative-threshold:0.85}")
    private double seedRelativeThreshold = 0.85d;

    public SearchResult execute(SearchSourceQuery query) {
        UUID userId = requestContextProvider.getRequestContext().userId();

        Question question = questionFor(query, userId);

        float[] queryVector = embeddingPort.embed(query.searchTerm());

        List<PassageCandidate> passageCandidates = semanticSearch.findPassageCandidate(queryVector, userId, semanticAnchorLimit);
        List<TripleCandidate> tripleCandidates = semanticSearch.findTripleCandidates(queryVector, userId, tripleCandidateLimit);

        List<GraphSeed> conceptSeeds = tripleCandidates.isEmpty()
                ? List.of()
                : List.copyOf(Optional.ofNullable(
                        recognitionMemory.recognize(query.searchTerm(), tripleCandidates, recognitionSeedLimit)).orElse(List.of()));

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
                GraphSearchRequest.from(userId, seeds, graphPassageLimit)
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
        return new AnswerSnapshot.RetrievalParameters(semanticAnchorLimit, tripleCandidateLimit, recognitionSeedLimit,
                graphPassageLimit, seedMinScore, seedRelativeThreshold);
    }

    private Question questionFor(SearchSourceQuery query, UUID userId) {
        if (query.questionId() != null) {
            return historyRepository.findQuestionByIdAndUserId(query.questionId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("Question does not belong to the authenticated user"));
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

        return Math.max(seedMinScore, bestScore * seedRelativeThreshold);
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
