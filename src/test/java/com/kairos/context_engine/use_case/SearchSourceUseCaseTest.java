package com.kairos.context_engine.use_case;

import com.kairos.context_engine.application.query.SearchSourceQuery;
import com.kairos.context_engine.application.use_case.SearchSourceUseCase;
import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.content.Source;
import com.kairos.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.context_engine.domain.port.graph.KnowledgeGraphSearch;
import com.kairos.context_engine.domain.model.*;
import com.kairos.context_engine.domain.port.semantic.SemanticSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchSourceUseCase")
class SearchSourceUseCaseTest {

    @Mock
    private EmbeddingProvider embeddingPort;

    @Mock
    private KnowledgeGraphSearch knowledgeGraphSearch;

    @Mock
    private SemanticSearch semanticSearch;

    @InjectMocks
    private SearchSourceUseCase useCase;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static final float[] QUERY_VECTOR = new float[]{0.1f, 0.2f, 0.3f};
    private static final String SEARCH_TERM   = "What is the philosophy of mind?";

    private Source dummySource;

    @BeforeEach
    void setUp() {
        dummySource = new Source(UUID.randomUUID(), "Philosophy of Mind - Chapter 1", "BLA BLA");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Chunk chunk(UUID id, String content, int index) {
        return new Chunk(id, dummySource, content, index, false, QUERY_VECTOR);
    }

    private KnowledgeTriple triple(UUID chunkId) {
        return KnowledgeTriple.create("Consciousness", "is_part_of", "Mind", chunkId);
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Nested
    @DisplayName("given a valid query with matching knowledge")
    class HappyPath {

        @Test
        @DisplayName("embeds the search term and passes the resulting vector to the semantic port")
        void embedsSearchTermAndForwardsVector() {
            var query   = new SearchSourceQuery(SEARCH_TERM);
            var anchor  = chunk(UUID.randomUUID(), "Consciousness arises from neural activity.", 0);
            var triple  = triple(anchor.getId());

            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of(anchor));
            when(knowledgeGraphSearch.expandKnowledge(List.of(anchor))).thenReturn(List.of(triple));
            when(semanticSearch.findChunks(List.of(anchor.getId()))).thenReturn(List.of(anchor));

            useCase.execute(query);

            ArgumentCaptor<float[]> vectorCaptor = ArgumentCaptor.forClass(float[].class);
            verify(semanticSearch).findTopK(vectorCaptor.capture(), eq(10));
            assertThat(vectorCaptor.getValue()).isEqualTo(QUERY_VECTOR);
        }

        @Test
        @DisplayName("returns a non-empty SearchResult containing both triples and hydrated chunks")
        void returnsNonEmptyResultWithTriplesAndChunks() {
            var query  = new SearchSourceQuery(SEARCH_TERM);
            var anchor = chunk(UUID.randomUUID(), "The mind supervenes on the brain.", 0);
            var triple = triple(anchor.getId());

            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of(anchor));
            when(knowledgeGraphSearch.expandKnowledge(List.of(anchor))).thenReturn(List.of(triple));
            when(semanticSearch.findChunks(List.of(anchor.getId()))).thenReturn(List.of(anchor));

            SearchResult result = useCase.execute(query);

            assertThat(result).isNotNull();
            assertThat(result.knowledgeTriples()).containsExactly(triple);
            assertThat(result.chunks()).containsExactly(anchor);
        }

        @Test
        @DisplayName("passes all semantic anchors (not a subset) to the graph expansion phase")
        void passesAllAnchorsToGraphExpansion() {
            var query = new SearchSourceQuery(SEARCH_TERM);
            List<Chunk> anchors = List.of(
                    chunk(UUID.randomUUID(), "Chunk A", 0),
                    chunk(UUID.randomUUID(), "Chunk B", 1),
                    chunk(UUID.randomUUID(), "Chunk C", 2)
            );
            var triple = triple(anchors.getFirst().getId());

            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(anchors);
            when(knowledgeGraphSearch.expandKnowledge(anchors)).thenReturn(List.of(triple));
            when(semanticSearch.findChunks(anyList())).thenReturn(List.of(anchors.getFirst()));

            useCase.execute(query);

            verify(knowledgeGraphSearch).expandKnowledge(anchors);
        }
    }

    // =========================================================================
    // Short-circuit: no semantic anchors found
    // =========================================================================

    @Nested
    @DisplayName("given no semantic anchors are found")
    class NoSemanticAnchors {

        @Test
        @DisplayName("returns SearchResult.empty() immediately")
        void returnsEmptyResultWhenNoAnchors() {
            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of());

            SearchResult result = useCase.execute(new SearchSourceQuery(SEARCH_TERM));

            assertThat(result).isEqualTo(SearchResult.empty());
        }

        @Test
        @DisplayName("never calls the knowledge graph when no anchors are present")
        void neverCallsGraphWhenNoAnchors() {
            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of());

            useCase.execute(new SearchSourceQuery(SEARCH_TERM));

            verifyNoInteractions(knowledgeGraphSearch);
        }

        @Test
        @DisplayName("never calls findChunks when no anchors are present")
        void neverCallsFindChunksWhenNoAnchors() {
            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of());

            useCase.execute(new SearchSourceQuery(SEARCH_TERM));

            verify(semanticSearch, never()).findChunks(anyList());
        }
    }

    // =========================================================================
    // Ordering contract (PPR ranking must be preserved)
    // =========================================================================

    @Nested
    @DisplayName("graph-driven ordering contract")
    class OrderingContract {

        @Test
        @DisplayName("preserves PPR ordering — chunks are returned in the order triples dictate, not retrieval order")
        void preservesPprOrdering() {
            var idFirst  = UUID.randomUUID(); // highest PPR rank
            var idSecond = UUID.randomUUID();
            var idThird  = UUID.randomUUID(); // lowest PPR rank

            var chunkFirst  = chunk(idFirst,  "Most relevant passage.",  0);
            var chunkSecond = chunk(idSecond, "Second most relevant.",    1);
            var chunkThird  = chunk(idThird,  "Least relevant passage.", 2);

            // Triples arrive in PPR order: first → second → third
            List<KnowledgeTriple> orderedTriples = List.of(
                    triple(idFirst),
                    triple(idSecond),
                    triple(idThird)
            );

            // Semantic store returns chunks in reverse order (simulating unordered DB result)
            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of(chunkFirst));
            when(knowledgeGraphSearch.expandKnowledge(anyList())).thenReturn(orderedTriples);
            when(semanticSearch.findChunks(anyList())).thenReturn(
                    List.of(chunkThird, chunkSecond, chunkFirst)  // intentionally scrambled
            );

            SearchResult result = useCase.execute(new SearchSourceQuery(SEARCH_TERM));

            assertThat(result.chunks())
                    .extracting(Chunk::getId)
                    .containsExactly(idFirst, idSecond, idThird);
        }

        @Test
        @DisplayName("deduplicates chunk IDs when the same chunk appears in multiple triples")
        void deduplicatesChunkIdsAcrossTriples() {
            var sharedId  = UUID.randomUUID();
            var uniqueId  = UUID.randomUUID();
            var sharedChunk = chunk(sharedId, "Shared passage referenced twice.", 0);
            var uniqueChunk = chunk(uniqueId, "Unique passage.", 1);

            // Same chunkId referenced by two different triples (common in PPR expansion)
            List<KnowledgeTriple> triplesWithDuplicate = List.of(
                    KnowledgeTriple.create("Mind",    "has_property", "Consciousness", sharedId),
                    KnowledgeTriple.create("Qualia",  "relates_to",   "Experience",    sharedId),
                    KnowledgeTriple.create("Neuron",  "fires_during", "Thought",       uniqueId)
            );

            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of(sharedChunk));
            when(knowledgeGraphSearch.expandKnowledge(anyList())).thenReturn(triplesWithDuplicate);
            when(semanticSearch.findChunks(anyList())).thenReturn(List.of(sharedChunk, uniqueChunk));

            SearchResult result = useCase.execute(new SearchSourceQuery(SEARCH_TERM));

            // sharedId must appear only once in the hydrated context
            assertThat(result.chunks())
                    .extracting(Chunk::getId)
                    .containsExactly(sharedId, uniqueId);
        }

        @Test
        @DisplayName("hydration request includes exactly the deduplicated ordered IDs")
        void hydrationRequestContainsDeduplicatedIds() {
            var idA = UUID.randomUUID();
            var idB = UUID.randomUUID();
            var anchor = chunk(idA, "Anchor.", 0);

            List<KnowledgeTriple> triples = List.of(
                    triple(idA),
                    triple(idB),
                    triple(idA) // duplicate
            );

            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of(anchor));
            when(knowledgeGraphSearch.expandKnowledge(anyList())).thenReturn(triples);
            when(semanticSearch.findChunks(anyList())).thenReturn(List.of());

            useCase.execute(new SearchSourceQuery(SEARCH_TERM));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<UUID>> idsCaptor = ArgumentCaptor.forClass(List.class);
            verify(semanticSearch).findChunks(idsCaptor.capture());

            assertThat(idsCaptor.getValue())
                    .containsExactly(idA, idB)
                    .doesNotHaveDuplicates();
        }
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("returns empty context gracefully when graph expansion yields no triples")
        void handlesEmptyTriplesFromGraphExpansion() {
            var anchor = chunk(UUID.randomUUID(), "Orphan chunk with no graph connections.", 0);

            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of(anchor));
            when(knowledgeGraphSearch.expandKnowledge(anyList())).thenReturn(List.of());

            SearchResult result = useCase.execute(new SearchSourceQuery(SEARCH_TERM));

            assertThat(result.chunks()).isEmpty();
            assertThat(result.knowledgeTriples()).isEmpty();
            verify(semanticSearch, never()).findChunks(anyList());
        }

        @Test
        @DisplayName("tolerates null entries in findChunks result without NullPointerException")
        void toleratesNullEntriesInHydrationResult() {
            // If the store is missing a chunk (e.g., deleted after indexing), chunkMap.get() returns null.
            // The current implementation propagates null into the list — this test documents that behaviour
            // and ensures no unexpected exception is thrown at the use-case boundary.
            var idPresent = UUID.randomUUID();
            var idMissing = UUID.randomUUID();
            var presentChunk = chunk(idPresent, "Present chunk.", 0);

            List<KnowledgeTriple> triples = List.of(triple(idPresent), triple(idMissing));

            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of(presentChunk));
            when(knowledgeGraphSearch.expandKnowledge(anyList())).thenReturn(triples);
            when(semanticSearch.findChunks(anyList())).thenReturn(List.of(presentChunk)); // idMissing absent

            // Should not throw
            SearchResult result = useCase.execute(new SearchSourceQuery(SEARCH_TERM));

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("always requests exactly top-10 anchors from the semantic port")
        void alwaysRequestsTopTenAnchors() {
            when(embeddingPort.embed(any())).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(any(), eq(10))).thenReturn(List.of());

            useCase.execute(new SearchSourceQuery("any term"));

            verify(semanticSearch).findTopK(any(), eq(10));
        }

        @Test
        @DisplayName("single-chunk, single-triple scenario returns correct minimal result")
        void singleChunkSingleTriple() {
            var id     = UUID.randomUUID();
            var single = chunk(id, "The only result.", 0);
            var triple = triple(id);

            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of(single));
            when(knowledgeGraphSearch.expandKnowledge(List.of(single))).thenReturn(List.of(triple));
            when(semanticSearch.findChunks(List.of(id))).thenReturn(List.of(single));

            SearchResult result = useCase.execute(new SearchSourceQuery(SEARCH_TERM));

            assertThat(result.knowledgeTriples()).hasSize(1);
            assertThat(result.chunks()).hasSize(1);
            assertThat(result.chunks().getFirst().getId()).isEqualTo(id);
        }
    }

    // =========================================================================
    // Failure / propagation
    // =========================================================================

    @Nested
    @DisplayName("error propagation")
    class ErrorPropagation {

        @Test
        @DisplayName("propagates RuntimeException from EmbeddingProvider without wrapping")
        void propagatesEmbeddingProviderException() {
            when(embeddingPort.embed(any())).thenThrow(new RuntimeException("Embedding model unavailable"));

            assertThatThrownBy(() -> useCase.execute(new SearchSourceQuery(SEARCH_TERM)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Embedding model unavailable");
        }

        @Test
        @DisplayName("propagates RuntimeException from KnowledgeGraphSearch without wrapping")
        void propagatesGraphSearchException() {
            var anchor = chunk(UUID.randomUUID(), "Anchor.", 0);

            when(embeddingPort.embed(any())).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(any(), anyInt())).thenReturn(List.of(anchor));
            when(knowledgeGraphSearch.expandKnowledge(anyList()))
                    .thenThrow(new RuntimeException("Neo4j connection refused"));

            assertThatThrownBy(() -> useCase.execute(new SearchSourceQuery(SEARCH_TERM)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Neo4j connection refused");
        }

        @Test
        @DisplayName("propagates RuntimeException from SemanticSearch.findChunks without wrapping")
        void propagatesFindChunksException() {
            var anchor = chunk(UUID.randomUUID(), "Anchor.", 0);
            var triple = triple(anchor.getId());

            when(embeddingPort.embed(any())).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(any(), anyInt())).thenReturn(List.of(anchor));
            when(knowledgeGraphSearch.expandKnowledge(anyList())).thenReturn(List.of(triple));
            when(semanticSearch.findChunks(anyList()))
                    .thenThrow(new RuntimeException("pgvector query timeout"));

            assertThatThrownBy(() -> useCase.execute(new SearchSourceQuery(SEARCH_TERM)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("pgvector query timeout");
        }
    }

    // =========================================================================
    // Interaction protocol (call count / order)
    // =========================================================================

    @Nested
    @DisplayName("interaction protocol")
    class InteractionProtocol {

        @Test
        @DisplayName("each dependency is called exactly once per execution")
        void eachDependencyCalledExactlyOnce() {
            var anchor = chunk(UUID.randomUUID(), "Anchor.", 0);
            var triple = triple(anchor.getId());

            when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(QUERY_VECTOR, 10)).thenReturn(List.of(anchor));
            when(knowledgeGraphSearch.expandKnowledge(anyList())).thenReturn(List.of(triple));
            when(semanticSearch.findChunks(anyList())).thenReturn(List.of(anchor));

            useCase.execute(new SearchSourceQuery(SEARCH_TERM));

            verify(embeddingPort, times(1)).embed(any());
            verify(semanticSearch, times(1)).findTopK(any(), anyInt());
            verify(knowledgeGraphSearch, times(1)).expandKnowledge(anyList());
            verify(semanticSearch, times(1)).findChunks(anyList());
        }

        @Test
        @DisplayName("findChunks is never called when graph expansion returns no triples")
        void findChunksNotCalledWhenNoTriples() {
            var anchor = chunk(UUID.randomUUID(), "Anchor.", 0);

            when(embeddingPort.embed(any())).thenReturn(QUERY_VECTOR);
            when(semanticSearch.findTopK(any(), anyInt())).thenReturn(List.of(anchor));
            when(knowledgeGraphSearch.expandKnowledge(anyList())).thenReturn(List.of());

            useCase.execute(new SearchSourceQuery(SEARCH_TERM));

            verify(semanticSearch, never()).findChunks(anyList());
        }
    }
}
