package com.kairos.context_engine.infrastructure.semantic;

import com.kairos.context_engine.domain.model.Chunk;
import com.kairos.context_engine.domain.model.SourceStatus;
import com.kairos.context_engine.infrastructure.persistence.entity.relation.ChunkEntity;
import com.kairos.context_engine.infrastructure.persistence.entity.relation.SourceEntity;
import com.kairos.context_engine.infrastructure.persistence.repository.relation.chunk.JpaChunkRepository;
import com.kairos.context_engine.infrastructure.persistence.repository.relation.source.JpaSourceRepository;
import com.kairos.context_engine.infrastructure.semantic.SemanticSearchAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SemanticSearchAdapter")
class SemanticSearchAdapterTest {

    @Mock
    private JpaSourceRepository jpaSourceRepository;

    @Mock
    private JpaChunkRepository jpaChunkRepository;

    @InjectMocks
    private SemanticSearchAdapter adapter;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static final float[] QUERY_VECTOR = {0.1f, 0.2f, 0.3f, 0.4f};

    private SourceEntity sourceEntityA;
    private SourceEntity sourceEntityB;

    @BeforeEach
    void setUp() {
        sourceEntityA = new SourceEntity(
                UUID.randomUUID(), "Philosophy of Mind", "Content A", SourceStatus.COMPLETED.name()
        );
        sourceEntityB = new SourceEntity(
                UUID.randomUUID(), "Cognitive Science", "Content B", SourceStatus.COMPLETED.name()
        );
    }

    // Helper: builds a ChunkEntity stub backed by a given SourceEntity
    private ChunkEntity chunkEntity(UUID id, SourceEntity source, String content, int index) {
        ChunkEntity entity = mock(ChunkEntity.class);
        when(entity.getSource()).thenReturn(source);
        when(entity.getContent()).thenReturn(content);
        when(entity.getIndex()).thenReturn(index);
        when(entity.getEmbedding()).thenReturn(QUERY_VECTOR);
        return entity;
    }

    // =========================================================================
    // search()
    // =========================================================================



    // =========================================================================
    // findTopK()
    // =========================================================================

    @Nested
    @DisplayName("findTopK(float[], int)")
    class FindTopKMethod {

        @Test
        @DisplayName("forwards the query vector and k to jpaChunkRepository unchanged")
        void forwardsVectorAndKToRepository() {
            when(jpaChunkRepository.findTopKByEmbedding(QUERY_VECTOR, 10)).thenReturn(List.of());

            adapter.findTopK(QUERY_VECTOR, 10);

            ArgumentCaptor<float[]> vectorCaptor = ArgumentCaptor.forClass(float[].class);
            verify(jpaChunkRepository).findTopKByEmbedding(vectorCaptor.capture(), eq(10));
            assertThat(vectorCaptor.getValue()).isEqualTo(QUERY_VECTOR);
        }

        @Test
        @DisplayName("maps each ChunkEntity to a Chunk domain model preserving content and index")
        void mapsChunkEntitiesToDomainModels() {
            ChunkEntity entityA = chunkEntity(UUID.randomUUID(), sourceEntityA, "Chunk text A", 0);
            ChunkEntity entityB = chunkEntity(UUID.randomUUID(), sourceEntityB, "Chunk text B", 1);

            when(jpaChunkRepository.findTopKByEmbedding(any(), anyInt()))
                    .thenReturn(List.of(entityA, entityB));

            List<Chunk> result = adapter.findTopK(QUERY_VECTOR, 2);

            assertThat(result).hasSize(2);
            assertThat(result.getFirst().getContent()).isEqualTo("Chunk text A");
            assertThat(result.getFirst().getIndex()).isEqualTo(0);
            assertThat(result.get(1).getContent()).isEqualTo("Chunk text B");
            assertThat(result.get(1).getIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("hydrates each Chunk with the embedding from its entity")
        void hydratesChunkWithEmbedding() {
            ChunkEntity entity = chunkEntity(UUID.randomUUID(), sourceEntityA, "Some text", 0);

            when(jpaChunkRepository.findTopKByEmbedding(any(), anyInt()))
                    .thenReturn(List.of(entity));

            List<Chunk> result = adapter.findTopK(QUERY_VECTOR, 1);

            assertThat(result.getFirst().getEmbedding()).isEqualTo(QUERY_VECTOR);
        }

        @Test
        @DisplayName("hydrates each Chunk with the correct parent Source")
        void hydratesChunkWithCorrectSource() {
            ChunkEntity entity = chunkEntity(UUID.randomUUID(), sourceEntityA, "Some text", 0);

            when(jpaChunkRepository.findTopKByEmbedding(any(), anyInt()))
                    .thenReturn(List.of(entity));

            List<Chunk> result = adapter.findTopK(QUERY_VECTOR, 1);

            assertThat(result.getFirst().getSource().getId()).isEqualTo(sourceEntityA.getId());
            assertThat(result.getFirst().getSource().getTitle()).isEqualTo(sourceEntityA.getTitle());
        }

        @Test
        @DisplayName("preserves the cosine-distance ranking order returned by the repository")
        void preservesRankingOrder() {
            ChunkEntity first  = chunkEntity(UUID.randomUUID(), sourceEntityA, "Highest relevance", 0);
            ChunkEntity second = chunkEntity(UUID.randomUUID(), sourceEntityA, "Medium relevance",  1);
            ChunkEntity third  = chunkEntity(UUID.randomUUID(), sourceEntityA, "Lowest relevance",  2);

            when(jpaChunkRepository.findTopKByEmbedding(any(), anyInt()))
                    .thenReturn(List.of(first, second, third));

            List<Chunk> result = adapter.findTopK(QUERY_VECTOR, 3);

            assertThat(result)
                    .extracting(Chunk::getContent)
                    .containsExactly("Highest relevance", "Medium relevance", "Lowest relevance");
        }

        @Test
        @DisplayName("returns an empty list when the repository finds no matching chunks")
        void returnsEmptyListWhenNoResults() {
            when(jpaChunkRepository.findTopKByEmbedding(any(), anyInt())).thenReturn(List.of());

            List<Chunk> result = adapter.findTopK(QUERY_VECTOR, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("never interacts with jpaSourceRepository during findTopK")
        void doesNotTouchSourceRepositoryDuringFindTopK() {
            when(jpaChunkRepository.findTopKByEmbedding(any(), anyInt())).thenReturn(List.of());

            adapter.findTopK(QUERY_VECTOR, 10);

            verifyNoInteractions(jpaSourceRepository);
        }

        @Test
        @DisplayName("propagates RuntimeException from jpaChunkRepository without wrapping")
        void propagatesRepositoryException() {
            when(jpaChunkRepository.findTopKByEmbedding(any(), anyInt()))
                    .thenThrow(new RuntimeException("Connection pool exhausted"));

            assertThatThrownBy(() -> adapter.findTopK(QUERY_VECTOR, 10))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Connection pool exhausted");
        }
    }

    // =========================================================================
    // findChunks()
    // =========================================================================

    @Nested
    @DisplayName("findChunks(List<UUID>)")
    class FindChunksMethod {

        @Test
        @DisplayName("returns empty list immediately when given a null ID list")
        void returnsEmptyForNullInput() {
            List<Chunk> result = adapter.findChunks(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(jpaChunkRepository);
        }

        @Test
        @DisplayName("returns empty list immediately when given an empty ID list")
        void returnsEmptyForEmptyInput() {
            List<Chunk> result = adapter.findChunks(List.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(jpaChunkRepository);
        }

        @Test
        @DisplayName("delegates to jpaChunkRepository.findAllById with the exact IDs provided")
        void delegatesWithExactIds() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();
            List<UUID> ids = List.of(idA, idB);

            when(jpaChunkRepository.findAllById(ids)).thenReturn(List.of());

            adapter.findChunks(ids);

            verify(jpaChunkRepository).findAllById(ids);
        }

        @Test
        @DisplayName("maps each retrieved ChunkEntity to a domain Chunk")
        void mapsFetchedEntitiesToDomainModels() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();

            ChunkEntity entityA = chunkEntity(idA, sourceEntityA, "First chunk",  0);
            ChunkEntity entityB = chunkEntity(idB, sourceEntityA, "Second chunk", 1);

            when(jpaChunkRepository.findAllById(anyList()))
                    .thenReturn(List.of(entityA, entityB));

            List<Chunk> result = adapter.findChunks(List.of(idA, idB));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Chunk::getContent)
                    .containsExactlyInAnyOrder("First chunk", "Second chunk");
        }

        @Test
        @DisplayName("returns partial results when the store does not contain all requested IDs")
        void returnsPartialResultsForMissingIds() {
            UUID presentId = UUID.randomUUID();
            UUID missingId = UUID.randomUUID();

            ChunkEntity presentEntity = chunkEntity(presentId, sourceEntityA, "Present", 0);

            // Store returns only the chunk that exists
            when(jpaChunkRepository.findAllById(anyList()))
                    .thenReturn(List.of(presentEntity));

            List<Chunk> result = adapter.findChunks(List.of(presentId, missingId));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getContent()).isEqualTo("Present");
        }

        @Test
        @DisplayName("does not guarantee ordering — result order matches what the repository returns")
        void doesNotGuaranteeOrdering() {
            // The Javadoc explicitly states order is not guaranteed.
            // This test documents and verifies that the adapter does NOT impose its own sort.
            UUID idFirst  = UUID.randomUUID();
            UUID idSecond = UUID.randomUUID();

            ChunkEntity entitySecond = chunkEntity(idSecond, sourceEntityA, "Second in DB", 1);
            ChunkEntity entityFirst  = chunkEntity(idFirst,  sourceEntityA, "First in DB",  0);

            // Repository deliberately returns them in reverse order
            when(jpaChunkRepository.findAllById(anyList()))
                    .thenReturn(List.of(entitySecond, entityFirst));

            List<Chunk> result = adapter.findChunks(List.of(idFirst, idSecond));

            assertThat(result)
                    .extracting(Chunk::getContent)
                    .containsExactly("Second in DB", "First in DB");
        }

        @Test
        @DisplayName("handles a single-element ID list correctly")
        void handlesSingleId() {
            UUID id = UUID.randomUUID();
            ChunkEntity entity = chunkEntity(id, sourceEntityA, "Single chunk", 0);

            when(jpaChunkRepository.findAllById(List.of(id))).thenReturn(List.of(entity));

            List<Chunk> result = adapter.findChunks(List.of(id));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getContent()).isEqualTo("Single chunk");
        }

        @Test
        @DisplayName("returns empty list when none of the requested IDs exist in the store")
        void returnsEmptyWhenNoneFound() {
            when(jpaChunkRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

            List<Chunk> result = adapter.findChunks(List.of(UUID.randomUUID(), UUID.randomUUID()));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("never interacts with jpaSourceRepository during findChunks")
        void doesNotTouchSourceRepository() {
            when(jpaChunkRepository.findAllById(anyList())).thenReturn(List.of());

            adapter.findChunks(List.of(UUID.randomUUID()));

            verifyNoInteractions(jpaSourceRepository);
        }

        @Test
        @DisplayName("propagates RuntimeException from jpaChunkRepository.findAllById without wrapping")
        void propagatesRepositoryException() {
            when(jpaChunkRepository.findAllById(anyList()))
                    .thenThrow(new RuntimeException("Deadlock detected"));

            assertThatThrownBy(() -> adapter.findChunks(List.of(UUID.randomUUID())))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Deadlock detected");
        }
    }

    // =========================================================================
    // Cross-method isolation
    // =========================================================================

    @Nested
    @DisplayName("cross-method isolation")
    class CrossMethodIsolation {


        @Test
        @DisplayName("findTopK() and findChunks() each call only jpaChunkRepository, never jpaSourceRepository")
        void chunkMethodsDoNotTouchSourceRepository() {
            when(jpaChunkRepository.findTopKByEmbedding(any(), anyInt())).thenReturn(List.of());
            when(jpaChunkRepository.findAllById(anyList())).thenReturn(List.of());

            adapter.findTopK(QUERY_VECTOR, 5);
            adapter.findChunks(List.of(UUID.randomUUID()));

            verifyNoInteractions(jpaSourceRepository);
        }
    }
}