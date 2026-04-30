package com.kairos.context_engine.application.use_case;

import com.kairos.context_engine.application.query.SearchSourceQuery;
import com.kairos.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.context_engine.domain.port.graph.KnowledgeGraphSearch;
import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;
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
     * Executes the graph-augmented search query.
     *
     * @param query the search query containing the user's textual input.
     * @return a {@link SearchResult} containing the expanded context (hydrated text chunks)
     * and the semantic triples, strictly ordered by the graph convergence score.
     * Returns an empty result if no initial semantic anchors are found in the knowledge base.
     */
    public SearchResult execute(SearchSourceQuery query) {
        float[] queryVector = embeddingPort.embed(query.searchTerm());

        List<PassageCandidate> passageCandidates = semanticSearch.findPassageCandidate(queryVector, 10);

        if (passageCandidates.isEmpty()) return SearchResult.empty();

        List<KnowledgeTriple> triples = knowledgeGraphSearch.expandKnowledge(passageCandidates);

        List<UUID> orderedChunkIds = triples.stream()
                .map(triple -> triple.passage().chunkId())
                .distinct()
                .toList();

        List<Chunk> expandedContext = fetchAndSortExpandedContext(orderedChunkIds);

        return SearchResult.from(triples, expandedContext);
    }

    /**
     * Hydrates the chunk payloads from the semantic store and enforces the relevance ranking
     * established by the knowledge graph expansion phase.
     *
     * @param orderedChunkIds a list of UUIDs strictly ordered by graph relevance.
     * @return a list of fully hydrated {@link Chunk} objects preserving the input order.
     */
    private List<Chunk> fetchAndSortExpandedContext(List<UUID> orderedChunkIds) {
        if (orderedChunkIds.isEmpty()) return List.of();

        List<Chunk> unsortedChunks = semanticSearch.findChunks(orderedChunkIds);

        Map<UUID, Chunk> chunkMap = unsortedChunks.stream()
                .collect(Collectors.toMap(Chunk::getId, Function.identity()));

        return orderedChunkIds.stream()
                .map(chunkMap::get)
                .toList();
    }

}
