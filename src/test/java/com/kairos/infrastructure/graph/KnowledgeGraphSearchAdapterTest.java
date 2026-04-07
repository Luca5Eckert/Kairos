package com.kairos.infrastructure.graph;

import com.kairos.domain.graph.KnowledgeGraphSearch;
import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.KnowledgeTriple;
import com.kairos.infrastructure.persistence.repository.graph.Neo4jPassageNodeRepository;
import com.kairos.infrastructure.persistence.repository.graph.projection.GraphExpansionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeGraphSearchAdapter")
class KnowledgeGraphSearchAdapterTest {

    @Mock
    private Neo4jPassageNodeRepository passageNodeRepository;

    @InjectMocks
    private KnowledgeGraphSearchAdapter adapter;

    // ------------------------------------------------------------------ fixtures

    private static final UUID CHUNK_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CHUNK_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private Chunk chunkOf(UUID id) {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getId()).thenReturn(id);
        return chunk;
    }

    private GraphExpansionResult resultOf(String subject, String predicate, String object, UUID chunkId) {
        GraphExpansionResult result = mock(GraphExpansionResult.class);
        when(result.subject()).thenReturn(subject);
        when(result.predicate()).thenReturn(predicate);
        when(result.object()).thenReturn(object);
        when(result.chunkId()).thenReturn(chunkId.toString());
        return result;
    }

    // ================================================================== guard clause tests

    @Nested
    @DisplayName("expandKnowledge — empty / null input")
    class EmptyInputTests {

        @Test
        @DisplayName("returns empty list when semanticAnchors is null")
        void expandKnowledge_nullAnchors_returnsEmptyList() {
            List<KnowledgeTriple> result = adapter.expandKnowledge(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(passageNodeRepository);
        }

        @Test
        @DisplayName("returns empty list when semanticAnchors is empty")
        void expandKnowledge_emptyAnchors_returnsEmptyList() {
            List<KnowledgeTriple> result = adapter.expandKnowledge(Collections.emptyList());

            assertThat(result).isEmpty();
            verifyNoInteractions(passageNodeRepository);
        }
    }

    // ================================================================== delegation tests

    @Nested
    @DisplayName("expandKnowledge — repository delegation")
    class RepositoryDelegationTests {

        @Test
        @DisplayName("passes correct anchor IDs as strings to repository")
        void expandKnowledge_validAnchors_delegatesCorrectAnchorIds() {
            Chunk chunk1 = chunkOf(CHUNK_ID_1);
            Chunk chunk2 = chunkOf(CHUNK_ID_2);
            when(passageNodeRepository.expandKnowledgeFromAnchors(anyList()))
                    .thenReturn(Collections.emptyList());

            adapter.expandKnowledge(List.of(chunk1, chunk2));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
            verify(passageNodeRepository).expandKnowledgeFromAnchors(captor.capture());

            assertThat(captor.getValue())
                    .containsExactlyInAnyOrder(CHUNK_ID_1.toString(), CHUNK_ID_2.toString());
        }

        @Test
        @DisplayName("calls repository exactly once per invocation")
        void expandKnowledge_validAnchors_callsRepositoryExactlyOnce() {
            Chunk chunk = chunkOf(CHUNK_ID_1);
            when(passageNodeRepository.expandKnowledgeFromAnchors(anyList()))
                    .thenReturn(Collections.emptyList());

            adapter.expandKnowledge(List.of(chunk));

            verify(passageNodeRepository, times(1)).expandKnowledgeFromAnchors(anyList());
        }
    }

    // ================================================================== mapping tests

    @Nested
    @DisplayName("expandKnowledge — result mapping")
    class ResultMappingTests {

        @Test
        @DisplayName("maps single GraphExpansionResult to KnowledgeTriple correctly")
        void expandKnowledge_singleResult_mapsAllFieldsCorrectly() {
            Chunk chunk = chunkOf(CHUNK_ID_1);
            GraphExpansionResult raw = resultOf("Newton", "discovered", "Gravity", CHUNK_ID_1);
            when(passageNodeRepository.expandKnowledgeFromAnchors(anyList()))
                    .thenReturn(List.of(raw));

            List<KnowledgeTriple> result = adapter.expandKnowledge(List.of(chunk));

            assertThat(result).hasSize(1);
            KnowledgeTriple triple = result.get(0);
            assertThat(triple.subject().name()).isEqualTo("Newton");
            assertThat(triple.subject().centrality()).isZero();
            assertThat(triple.subject().degree()).isZero();
            assertThat(triple.predicate()).isEqualTo("discovered");
            assertThat(triple.object().name()).isEqualTo("Gravity");
            assertThat(triple.chunkId()).isEqualTo(CHUNK_ID_1);
        }

        @Test
        @DisplayName("maps multiple GraphExpansionResults preserving order")
        void expandKnowledge_multipleResults_preservesOrder() {
            Chunk chunk = chunkOf(CHUNK_ID_1);
            GraphExpansionResult raw1 = resultOf("A", "rel1", "B", CHUNK_ID_1);
            GraphExpansionResult raw2 = resultOf("C", "rel2", "D", CHUNK_ID_2);
            when(passageNodeRepository.expandKnowledgeFromAnchors(anyList()))
                    .thenReturn(List.of(raw1, raw2));

            List<KnowledgeTriple> result = adapter.expandKnowledge(List.of(chunk));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).subject().name()).isEqualTo("A");
            assertThat(result.get(1).subject().name()).isEqualTo("C");
        }

        @Test
        @DisplayName("returns empty list when repository returns no results")
        void expandKnowledge_repositoryReturnsEmpty_returnsEmptyList() {
            Chunk chunk = chunkOf(CHUNK_ID_1);
            when(passageNodeRepository.expandKnowledgeFromAnchors(anyList()))
                    .thenReturn(Collections.emptyList());

            List<KnowledgeTriple> result = adapter.expandKnowledge(List.of(chunk));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("correctly parses chunkId UUID string from repository result")
        void expandKnowledge_result_parsesChunkIdUuidFromString() {
            Chunk chunk = chunkOf(CHUNK_ID_2);
            GraphExpansionResult raw = resultOf("Einstein", "formulated", "Relativity", CHUNK_ID_2);
            when(passageNodeRepository.expandKnowledgeFromAnchors(anyList()))
                    .thenReturn(List.of(raw));

            List<KnowledgeTriple> result = adapter.expandKnowledge(List.of(chunk));

            assertThat(result.get(0).chunkId()).isEqualTo(CHUNK_ID_2);
        }
    }

    // ================================================================== anchor ID extraction tests

    @Nested
    @DisplayName("expandKnowledge — anchor ID extraction")
    class AnchorIdExtractionTests {

        @Test
        @DisplayName("extracts UUID toString from each chunk correctly")
        void expandKnowledge_extractsUuidAsStringFromChunks() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Chunk c1 = chunkOf(id1);
            Chunk c2 = chunkOf(id2);
            when(passageNodeRepository.expandKnowledgeFromAnchors(anyList()))
                    .thenReturn(Collections.emptyList());

            adapter.expandKnowledge(List.of(c1, c2));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
            verify(passageNodeRepository).expandKnowledgeFromAnchors(captor.capture());

            assertThat(captor.getValue())
                    .containsExactlyInAnyOrder(id1.toString(), id2.toString());
        }

        @Test
        @DisplayName("single chunk produces list with exactly one anchor ID")
        void expandKnowledge_singleChunk_producesOneAnchorId() {
            Chunk chunk = chunkOf(CHUNK_ID_1);
            when(passageNodeRepository.expandKnowledgeFromAnchors(anyList()))
                    .thenReturn(Collections.emptyList());

            adapter.expandKnowledge(List.of(chunk));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
            verify(passageNodeRepository).expandKnowledgeFromAnchors(captor.capture());

            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0)).isEqualTo(CHUNK_ID_1.toString());
        }
    }

    // ================================================================== contract / interface tests

    @Nested
    @DisplayName("KnowledgeGraphSearch contract")
    class ContractTests {

        @Test
        @DisplayName("adapter implements KnowledgeGraphSearch port")
        void adapter_implementsKnowledgeGraphSearchPort() {
            assertThat(adapter).isInstanceOf(KnowledgeGraphSearch.class);
        }

        @Test
        @DisplayName("returned list is unmodifiable (immutable)")
        void expandKnowledge_returnsImmutableList() {
            Chunk chunk = chunkOf(CHUNK_ID_1);
            GraphExpansionResult raw = resultOf("X", "rel", "Y", CHUNK_ID_1);
            when(passageNodeRepository.expandKnowledgeFromAnchors(anyList()))
                    .thenReturn(List.of(raw));

            List<KnowledgeTriple> result = adapter.expandKnowledge(List.of(chunk));

            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> result.add(mock(KnowledgeTriple.class))
            );
        }
    }
}