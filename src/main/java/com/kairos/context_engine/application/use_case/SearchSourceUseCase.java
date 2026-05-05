package com.kairos.context_engine.application.use_case;

import com.kairos.context_engine.application.query.SearchSourceQuery;
import com.kairos.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchResult;
import com.kairos.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.context_engine.domain.model.retrieval.seed.PassageSeedTarget;
import com.kairos.context_engine.domain.model.retrieval.seed.SeedType;
import com.kairos.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.context_engine.domain.port.graph.KnowledgeGraphSearch;
import com.kairos.context_engine.domain.model.SearchResult;
import com.kairos.context_engine.domain.port.semantic.SemanticSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * Executes a search query against the knowledge graph, returning a ranked list of relevant chunks and activated triples.
     * @param query the search query containing the search term and any additional parameters
     * @return a SearchResult containing the activated triples from the knowledge graph and a ranked list of relevant chunks based on the search query
     */
    public SearchResult execute(SearchSourceQuery query) {
        float[] queryVector = embeddingPort.embed(query.searchTerm());

        List<PassageCandidate> passageCandidates = semanticSearch.findPassageCandidate(queryVector, 10);

        var seeds = instanceSeedsFromCandidates(passageCandidates);

        var graphSearchRequest = GraphSearchRequest.from(seeds, 20);

        GraphSearchResult result = knowledgeGraphSearch.expandKnowledge(graphSearchRequest);

        List<RankedChunk> rankedChunks = semanticSearch.hydrateAndRankChunks(result.scoredPassages());

        return SearchResult.from(result.activatedTriples(), rankedChunks);
    }


    private List<GraphSeed> instanceSeedsFromCandidates(List<PassageCandidate> passageCandidates) {
        return passageCandidates.stream()
                .map(candidate -> new GraphSeed(new PassageSeedTarget(candidate.chunkId()), SeedType.PASSAGE, candidate.denseScore()))
                .toList();

    }

}
