package com.kairos.context_engine.application.use_case;

import com.kairos.context_engine.application.query.SearchSourceQuery;
import com.kairos.context_engine.domain.model.retrieval.candidate.ConceptCandidate;
import com.kairos.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchResult;
import com.kairos.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.context_engine.domain.port.graph.KnowledgeGraphSearch;
import com.kairos.context_engine.domain.model.SearchResult;
import com.kairos.context_engine.domain.port.semantic.SemanticSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
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

    @Value("${kairos.retrieval.semantic-anchor-limit:10}")
    private int semanticAnchorLimit = 10;

    @Value("${kairos.retrieval.graph-passage-limit:20}")
    private int graphPassageLimit = 20;

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
        float[] queryVector = embeddingPort.embed(query.searchTerm());

        List<PassageCandidate> passageCandidates = semanticSearch.findPassageCandidate(queryVector, semanticAnchorLimit);
        List<ConceptCandidate> conceptCandidates = semanticSearch.findConceptCandidate(queryVector, semanticAnchorLimit);

        var seeds = instanceSeedsFromCandidates(passageCandidates, conceptCandidates);

        if (seeds.isEmpty()) {
            return SearchResult.empty();
        }

        var graphSearchRequest = GraphSearchRequest.from(seeds, graphPassageLimit);

        GraphSearchResult result = knowledgeGraphSearch.expandKnowledge(graphSearchRequest);

        List<RankedChunk> rankedChunks = result.scoredPassages().isEmpty()
                ? List.of()
                : semanticSearch.hydrateAndRankChunks(result.scoredPassages());

        return SearchResult.from(result.activatedTriples(), rankedChunks);
    }


    private List<GraphSeed> instanceSeedsFromCandidates(List<PassageCandidate> passageCandidates, List<ConceptCandidate> conceptCandidates) {
        double passageThreshold = seedThreshold(
                passageCandidates.stream()
                        .mapToDouble(PassageCandidate::denseScore)
                        .max()
                        .orElse(0d)
        );
        double conceptThreshold = seedThreshold(
                conceptCandidates.stream()
                        .mapToDouble(ConceptCandidate::similarityScore)
                        .max()
                        .orElse(0d)
        );

        var passageSeed = passageCandidates.stream()
                .filter(candidate -> candidate.denseScore() >= passageThreshold)
                .map(candidate -> GraphSeed.passage(candidate.chunkId(), candidate.denseScore()));

        var conceptSeed = conceptCandidates.stream()
                .filter(candidate -> candidate.similarityScore() >= conceptThreshold)
                .map(candidate -> GraphSeed.concept(candidate.concept().name(), candidate.similarityScore()));

        return Stream.concat(passageSeed, conceptSeed).toList();
    }

    private double seedThreshold(double bestScore) {
        if (bestScore <= 0) {
            return Double.POSITIVE_INFINITY;
        }

        return Math.max(seedMinScore, bestScore * seedRelativeThreshold);
    }

}
