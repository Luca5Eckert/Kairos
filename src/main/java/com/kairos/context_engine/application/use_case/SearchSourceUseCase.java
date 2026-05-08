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

    private static final int SEMANTIC_ANCHOR_LIMIT = 10;
    private static final int GRAPH_PASSAGE_LIMIT = 20;

    private final EmbeddingProvider embeddingPort;
    private final KnowledgeGraphSearch knowledgeGraphSearch;
    private final SemanticSearch semanticSearch;

    /**
     * Executes a search query against the knowledge graph, returning a ranked list of relevant chunks and activated triples.
     * @param query the search query containing the search term and any additional parameters
     * @return a SearchResult containing the activated triples from the knowledge graph and a ranked list of relevant chunks based on the search query
     */
    public SearchResult execute(SearchSourceQuery query) {
        float[] queryVector = embeddingPort.embed(query.searchTerm());

        List<PassageCandidate> passageCandidates = semanticSearch.findPassageCandidate(queryVector, SEMANTIC_ANCHOR_LIMIT);
        List<ConceptCandidate> conceptCandidates = semanticSearch.findConceptCandidate(queryVector, SEMANTIC_ANCHOR_LIMIT);

        var seeds = instanceSeedsFromCandidates(passageCandidates, conceptCandidates);

        if (seeds.isEmpty()) {
            return SearchResult.empty();
        }

        var graphSearchRequest = GraphSearchRequest.from(seeds, GRAPH_PASSAGE_LIMIT);

        GraphSearchResult result = knowledgeGraphSearch.expandKnowledge(graphSearchRequest);

        List<RankedChunk> rankedChunks = result.scoredPassages().isEmpty()
                ? List.of()
                : semanticSearch.hydrateAndRankChunks(result.scoredPassages());

        return SearchResult.from(result.activatedTriples(), rankedChunks);
    }


    private List<GraphSeed> instanceSeedsFromCandidates(List<PassageCandidate> passageCandidates, List<ConceptCandidate> conceptCandidates) {
        var passageSeed = passageCandidates.stream()
                .filter(candidate -> candidate.denseScore() > 0)
                .map(candidate -> GraphSeed.passage(candidate.chunkId(), candidate.denseScore()));

        var conceptSeed = conceptCandidates.stream()
                .filter(candidate -> candidate.similarityScore() > 0)
                .map(candidate -> GraphSeed.concept(candidate.concept().name(), candidate.similarityScore()));

        return Stream.concat(passageSeed, conceptSeed).toList();
    }

}
