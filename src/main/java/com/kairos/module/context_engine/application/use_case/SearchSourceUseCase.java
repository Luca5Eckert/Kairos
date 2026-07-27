package com.kairos.module.context_engine.application.use_case;

import com.kairos.module.context_engine.application.query.SearchSourceQuery;
import com.kairos.module.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.module.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.candidate.TripleCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.module.context_engine.domain.model.retrieval.graph.GraphSearchResult;
import com.kairos.module.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.module.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.module.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.module.context_engine.domain.port.graph.KnowledgeGraphSearch;
import com.kairos.module.context_engine.domain.model.SearchResult;
import com.kairos.module.context_engine.domain.port.recognition.RecognitionMemory;
import com.kairos.module.context_engine.domain.port.semantic.SemanticSearch;
import com.kairos.share.security.context.RequestContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Orchestrates the HippoRAG 2 retrieval flow, combining dense vector search with
 * knowledge graph expansion to achieve multi-hop reasoning.
 * <p>
 * This use case follows a multiphase retrieval strategy:
 * <ol>
 * <li><b>Semantic Anchor Lookup:</b> Uses dense embeddings to find the most semantically relevant chunks in the vector store.</li>
 * <li><b>Graph Seeding & Expansion:</b> Uses the identified chunks as seeds for a Personalized PageRank (PPR) algorithm
 * over the knowledge graph, discovering structurally connected concepts and passages.</li>
 * <li><b>Hydration & Re-ranking:</b> Retrieves the full text payloads from the relational store while strictly preserving
 * the relevance ranking dictated by the graph's PPR convergence scores.</li>
 * </ol>
 * </p>
 */
@Component
@RequiredArgsConstructor
public class SearchSourceUseCase {

    private final EmbeddingProvider embeddingPort;
    private final KnowledgeGraphSearch knowledgeGraphSearch;
    private final SemanticSearch semanticSearch;
    private final RecognitionMemory recognitionMemory;
    private final RequestContextProvider requestContextProvider;

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

    /**
     * Executes a search query against the knowledge graph, returning a ranked list of relevant chunks and activated triples.
     * @param query the search query containing the search term and any additional parameters
     * @return a SearchResult containing the activated triples from the knowledge graph and a ranked list of relevant chunks based on the search query
     */
    public SearchResult execute(SearchSourceQuery query) {
        UUID userId = requestContextProvider.getRequestContext().userId();
        float[] queryVector = embeddingPort.embed(query.searchTerm());

        List<PassageCandidate> passageCandidates = semanticSearch.findPassageCandidate(queryVector, userId, semanticAnchorLimit);
        List<TripleCandidate> tripleCandidates = semanticSearch.findTripleCandidates(queryVector, userId, tripleCandidateLimit);
        List<GraphSeed> conceptSeeds = tripleCandidates.isEmpty()
                ? List.of()
                : recognitionMemory.recognize(query.searchTerm(), tripleCandidates, recognitionSeedLimit);

        var seeds = instanceSeedsFromCandidates(passageCandidates, conceptSeeds);

        if (seeds.isEmpty()) {
            return SearchResult.empty();
        }

        var graphSearchRequest = GraphSearchRequest.from(userId, seeds, graphPassageLimit);

        GraphSearchResult result = knowledgeGraphSearch.expandKnowledge(graphSearchRequest);

        List<RankedChunk> rankedChunks = result.scoredPassages().isEmpty()
                ? List.of()
                : semanticSearch.hydrateAndRankChunks(result.scoredPassages(), userId);

        return SearchResult.from(filterActivatedTriples(result.activatedTriples(), rankedChunks), rankedChunks);
    }


    private List<GraphSeed> instanceSeedsFromCandidates(List<PassageCandidate> passageCandidates, List<GraphSeed> conceptSeeds) {
        double passageThreshold = seedThreshold(
                passageCandidates.stream()
                        .mapToDouble(PassageCandidate::denseScore)
                        .max()
                        .orElse(0d)
        );

        var passageSeed = passageCandidates.stream()
                .filter(candidate -> candidate.denseScore() >= passageThreshold)
                .map(candidate -> GraphSeed.passage(candidate.chunkId(), candidate.denseScore()));

        double conceptThreshold = seedThreshold(
                conceptSeeds == null
                        ? 0d
                        : conceptSeeds.stream()
                                .mapToDouble(GraphSeed::weight)
                                .max()
                                .orElse(0d)
        );

        var conceptSeed = conceptSeeds == null
                ? Stream.<GraphSeed>empty()
                : conceptSeeds.stream()
                        .filter(seed -> seed.weight() >= conceptThreshold);

        return Stream.concat(passageSeed, conceptSeed).toList();
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

        Set<UUID> selectedChunkIds = rankedChunks.stream()
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
