package com.kairos.infrastructure.graph;

import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.KnowledgeTriple;
import com.kairos.infrastructure.persistence.repository.graph.projection.GraphExpansionResult;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeGraphSearchAdapter")
class KnowledgeGraphSearchAdapterTest {

    @Mock
    private KnowledgeGraphGdsExecutor gdsExecutor;

    @InjectMocks
    private KnowledgeGraphSearchAdapter adapter;

    // ═════════════════════════════════════════════════════════════════════════
    // expandKnowledge — guard cases
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("given null or empty anchors")
    class GuardCases {

        @Test
        @DisplayName("returns empty list and never touches the repository when anchors are null")
        void nullAnchors_returnsEmptyWithoutRepositoryInteraction() {
            List<KnowledgeTriple> result = adapter.expandKnowledge(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(gdsExecutor);
        }

        @Test
        @DisplayName("returns empty list and never touches the repository when anchors are empty")
        void emptyAnchors_returnsEmptyWithoutRepositoryInteraction() {
            List<KnowledgeTriple> result = adapter.expandKnowledge(List.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(gdsExecutor);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // expandKnowledge — graph naming
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("graph projection naming")
    class GraphNaming {

        @Test
        @DisplayName("projects a graph whose name starts with 'hipporag-' followed by a valid UUID")
        void graphName_hasHipporagPrefixAndUUIDSuffix() {
            stubSuccessfulExpansion(List.of(expansionRow(UUID.randomUUID())));

            adapter.expandKnowledge(List.of(chunk()));

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(gdsExecutor).projectPhraseGraph(captor.capture());

            String graphName = captor.getValue();
            assertThat(graphName).startsWith("hipporag-");
            assertThatCode(() -> UUID.fromString(graphName.substring("hipporag-".length())))
                    .as("suffix after 'hipporag-' must be a valid UUID")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("uses the same graph name for project, PPR and drop within a single call")
        void singleCall_sameGraphNameUsedAcrossAllThreeOperations() {
            stubSuccessfulExpansion(List.of(expansionRow(UUID.randomUUID())));

            adapter.expandKnowledge(List.of(chunk()));

            ArgumentCaptor<String> projectCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> pprCaptor     = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> dropCaptor    = ArgumentCaptor.forClass(String.class);

            verify(gdsExecutor).projectPhraseGraph(projectCaptor.capture());
            verify(gdsExecutor).runPPRExpansion(pprCaptor.capture(), any(), anyInt(), anyDouble(), anyInt());
            verify(gdsExecutor).dropProjectedGraph(dropCaptor.capture());

            assertThat(projectCaptor.getValue())
                    .isEqualTo(pprCaptor.getValue())
                    .isEqualTo(dropCaptor.getValue());
        }

        @Test
        @DisplayName("generates a distinct graph name for each independent call")
        void twoCalls_produceDistinctGraphNames() {
            stubSuccessfulExpansion(List.of(expansionRow(UUID.randomUUID())));

            adapter.expandKnowledge(List.of(chunk()));
            adapter.expandKnowledge(List.of(chunk()));

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(gdsExecutor, times(2)).projectPhraseGraph(captor.capture());

            List<String> names = captor.getAllValues();
            assertThat(names.get(0)).isNotEqualTo(names.get(1));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // expandKnowledge — PPR parameters
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PPR parameters forwarding")
    class PPRParameters {

        @Test
        @DisplayName("forwards anchor chunk UUIDs as strings to runPPRExpansion")
        void anchorIds_passedAsStringRepresentations() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            stubSuccessfulExpansion(List.of(expansionRow(id1), expansionRow(id2)));

            adapter.expandKnowledge(List.of(chunkWithId(id1), chunkWithId(id2)));

            ArgumentCaptor<List<String>> idsCaptor = ArgumentCaptor.forClass(List.class);
            verify(gdsExecutor).runPPRExpansion(
                    anyString(), idsCaptor.capture(), anyInt(), anyDouble(), anyInt());

            assertThat(idsCaptor.getValue())
                    .containsExactlyInAnyOrder(id1.toString(), id2.toString());
        }

        @Test
        @DisplayName("uses MAX_ITERATIONS=20, DAMPING_FACTOR=0.85, PASSAGE_LIMIT=10")
        void pprConstants_matchExpectedValues() {
            stubSuccessfulExpansion(List.of(expansionRow(UUID.randomUUID())));

            adapter.expandKnowledge(List.of(chunk()));

            verify(gdsExecutor).runPPRExpansion(
                    anyString(), anyList(),
                    eq(20),
                    eq(0.85),
                    eq(10));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // expandKnowledge — result mapping
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("result mapping")
    class ResultMapping {

        @Test
        @DisplayName("maps subject, predicate, object and chunkId to KnowledgeTriple correctly")
        void singleRow_mappedToTripleCorrectly() {
            UUID chunkId = UUID.randomUUID();
            stubSuccessfulExpansion(List.of(
                    expansionRow("Paris", "CAPITAL_OF", "France", chunkId.toString())));

            List<KnowledgeTriple> triples = adapter.expandKnowledge(List.of(chunkWithId(chunkId)));

            assertThat(triples).hasSize(1);
            KnowledgeTriple triple = triples.get(0);
            assertThat(triple.subject().name()).isEqualTo("Paris");
            assertThat(triple.predicate()).isEqualTo("CAPITAL_OF");
            assertThat(triple.object().name()).isEqualTo("France");
            assertThat(triple.chunkId()).isEqualTo(chunkId);
        }

        @Test
        @DisplayName("maps multiple rows preserving order returned by the repository")
        void multipleRows_allMappedInOrder() {
            UUID chunkId1 = UUID.randomUUID();
            UUID chunkId2 = UUID.randomUUID();
            stubSuccessfulExpansion(List.of(
                    expansionRow("A", "rel1", "B", chunkId1.toString()),
                    expansionRow("C", "rel2", "D", chunkId2.toString())));

            List<KnowledgeTriple> triples = adapter.expandKnowledge(List.of(chunk()));

            assertThat(triples).hasSize(2);
            assertThat(triples.get(0).chunkId()).isEqualTo(chunkId1);
            assertThat(triples.get(1).chunkId()).isEqualTo(chunkId2);
        }

        @Test
        @DisplayName("returns empty list when PPR finds no seeds and repository returns empty")
        void repositoryReturnsEmpty_resultIsEmpty() {
            stubSuccessfulExpansion(List.of());

            List<KnowledgeTriple> result = adapter.expandKnowledge(List.of(chunk()));

            assertThat(result).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // expandKnowledge — null chunkId filtering
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("null chunkId filtering")
    class NullChunkIdFiltering {

        @Test
        @DisplayName("filters out rows with null chunkId and keeps the valid ones")
        void mixedRows_nullChunkIdRowsDropped() {
            UUID validId = UUID.randomUUID();
            stubSuccessfulExpansion(List.of(
                    expansionRow("X", "rel", "Y", null),
                    expansionRow("A", "rel", "B", validId.toString())));

            List<KnowledgeTriple> result = adapter.expandKnowledge(List.of(chunk()));

            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(t -> assertThat(t.chunkId()).isEqualTo(validId));
        }

        @Test
        @DisplayName("returns empty list when all rows have null chunkId")
        void allNullChunkIds_returnsEmpty() {
            stubSuccessfulExpansion(List.of(
                    expansionRow("X", "rel", "Y", null),
                    expansionRow("A", "rel", "B", null)));

            List<KnowledgeTriple> result = adapter.expandKnowledge(List.of(chunk()));

            assertThat(result).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // expandKnowledge — exception safety / GDS cleanup guarantee
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GDS projection cleanup guarantee")
    class CleanupGuarantee {

        @Test
        @DisplayName("drops the graph even when PPR throws, and re-throws the original exception")
        void pprThrows_graphDroppedAndExceptionPropagated() {
            RuntimeException pprFailure = new RuntimeException("GDS stream failure");
            doNothing().when(gdsExecutor).projectPhraseGraph(anyString());
            when(gdsExecutor.runPPRExpansion(anyString(), anyList(), anyInt(), anyDouble(), anyInt()))
                    .thenThrow(pprFailure);

            assertThatThrownBy(() -> adapter.expandKnowledge(List.of(chunk())))
                    .isSameAs(pprFailure);

            verify(gdsExecutor).dropProjectedGraph(anyString());
        }

        @Test
        @DisplayName("drops the graph even when projection itself throws, and re-throws the original exception")
        void projectThrows_dropCalledAndExceptionPropagated() {
            RuntimeException projectionFailure = new RuntimeException("GDS projection failure");
            doThrow(projectionFailure).when(gdsExecutor).projectPhraseGraph(anyString());

            assertThatThrownBy(() -> adapter.expandKnowledge(List.of(chunk())))
                    .isSameAs(projectionFailure);

            verify(gdsExecutor).dropProjectedGraph(anyString());
        }

        @Test
        @DisplayName("does not mask a successful result when the drop fails in finally")
        void dropFailsInFinally_successfulResultStillReturned() {
            UUID chunkId = UUID.randomUUID();
            stubSuccessfulExpansion(List.of(expansionRow("S", "p", "O", chunkId.toString())));
            doThrow(new RuntimeException("drop failed"))
                    .when(gdsExecutor).dropProjectedGraph(anyString());

            assertThatCode(() -> {
                List<KnowledgeTriple> result = adapter.expandKnowledge(List.of(chunk()));
                assertThat(result).hasSize(1);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("propagates the PPR exception when both PPR and drop fail")
        void pprAndDropBothFail_originalPPRExceptionPropagated() {
            RuntimeException pprFailure  = new RuntimeException("PPR failure");
            RuntimeException dropFailure = new RuntimeException("drop failure");

            doNothing().when(gdsExecutor).projectPhraseGraph(anyString());
            when(gdsExecutor.runPPRExpansion(anyString(), anyList(), anyInt(), anyDouble(), anyInt()))
                    .thenThrow(pprFailure);
            doThrow(dropFailure).when(gdsExecutor).dropProjectedGraph(anyString());

            assertThatThrownBy(() -> adapter.expandKnowledge(List.of(chunk())))
                    .isSameAs(pprFailure)
                    .isNotSameAs(dropFailure);
        }

        @Test
        @DisplayName("always drops the graph even when the result list is empty")
        void emptyResult_graphStillDropped() {
            stubSuccessfulExpansion(List.of());

            adapter.expandKnowledge(List.of(chunk()));

            verify(gdsExecutor).dropProjectedGraph(anyString());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // cleanupOrphanProjections
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cleanupOrphanProjections")
    class CleanupOrphanProjections {

        @Test
        @DisplayName("delegates to dropOrphanProjections and completes without exception when projections were removed")
        void orphansFound_completesNormally() {
            when(gdsExecutor.dropOrphanProjections())
                    .thenReturn(List.of("hipporag-abc", "hipporag-xyz"));

            assertThatCode(() -> adapter.cleanupOrphanProjections()).doesNotThrowAnyException();
            verify(gdsExecutor).dropOrphanProjections();
        }

        @Test
        @DisplayName("completes without exception when no orphan projections exist")
        void noOrphans_completesNormally() {
            when(gdsExecutor.dropOrphanProjections()).thenReturn(List.of());

            assertThatCode(() -> adapter.cleanupOrphanProjections()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("swallows repository exceptions to protect the scheduler thread")
        void repositoryThrows_exceptionSwallowed() {
            when(gdsExecutor.dropOrphanProjections())
                    .thenThrow(new RuntimeException("Neo4j unavailable"));

            assertThatCode(() -> adapter.cleanupOrphanProjections()).doesNotThrowAnyException();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private Chunk chunk() {
        return chunkWithId(UUID.randomUUID());
    }

    private Chunk chunkWithId(UUID id) {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getId()).thenReturn(id);
        return chunk;
    }

    private void stubSuccessfulExpansion(List<GraphExpansionResult> rows) {
        when(gdsExecutor.runPPRExpansion(anyString(), anyList(), anyInt(), anyDouble(), anyInt()))
                .thenReturn(rows);
    }

    /**
     * Creates a mock {@link GraphExpansionResult} with default subject/predicate/object
     * and the given chunkId — for tests that only care about the chunkId value.
     */
    private GraphExpansionResult expansionRow(UUID chunkId) {
        return expansionRow("Subject", "predicate", "Object",
                chunkId != null ? chunkId.toString() : null);
    }

    /**
     * Creates a mock {@link GraphExpansionResult} with null chunkId.
     * Used to test the null-chunkId filtering path.
     */
    private GraphExpansionResult expansionRow(String subject, String predicate,
                                              String object, String chunkId) {
        GraphExpansionResult row = mock(GraphExpansionResult.class);
        when(row.subject()).thenReturn(subject);
        when(row.predicate()).thenReturn(predicate);
        when(row.object()).thenReturn(object);
        when(row.chunkId()).thenReturn(chunkId);
        return row;
    }
}
