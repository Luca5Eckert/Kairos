package com.kairos.context_engine.infrastructure.graph.adapter;

import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchResult;
import com.kairos.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.context_engine.infrastructure.graph.executor.KnowledgeGraphGdsExecutor;
import com.kairos.context_engine.infrastructure.graph.repository.projection.GraphExpansionResult;
import com.kairos.context_engine.infrastructure.graph.repository.projection.PassageScoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HippoRagKnowledgeGraphSearchAdapter")
class HippoRagKnowledgeGraphSearchAdapterTest {

    @Mock
    private KnowledgeGraphGdsExecutor executor;

    private HippoRagKnowledgeGraphSearchAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new HippoRagKnowledgeGraphSearchAdapter(executor, 7, 0.9, 0.12);
    }

    @Test
    @DisplayName("returns empty result without touching GDS when request has no seeds")
    void emptySeedsReturnEmptyWithoutExecutorInteraction() {
        GraphSearchResult result = adapter.expandKnowledge(GraphSearchRequest.from(List.of(), 20));

        assertThat(result).isEqualTo(GraphSearchResult.empty());
        verifyNoInteractions(executor);
    }

    @Test
    @DisplayName("separates passage and concept seeds before running PPR")
    void separatesPassageAndConceptSeeds() {
        UUID passageId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        when(executor.runPPRPassageScores(anyString(), anyList(), anyList(), anyInt(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(new PassageScoringResult(resultId.toString(), 0.73)));
        when(executor.runPPRActivatedTriples(anyString(), anyList(), anyList(), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of(row("Mind", "relates_to", "Brain", resultId.toString(), 0.73, 0.4)));

        adapter.expandKnowledge(GraphSearchRequest.from(List.of(
                GraphSeed.passage(passageId, 0.91),
                GraphSeed.concept("Mind", 0.8)
        ), 20));

        ArgumentCaptor<List<String>> passageIds = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> conceptNames = ArgumentCaptor.forClass(List.class);

        verify(executor).runPPRPassageScores(
                anyString(),
                passageIds.capture(),
                conceptNames.capture(),
                eq(7),
                eq(0.9),
                eq(0.12),
                eq(20)
        );
        verify(executor).runPPRActivatedTriples(
                anyString(),
                eq(passageIds.getValue()),
                eq(conceptNames.getValue()),
                eq(7),
                eq(0.9),
                eq(0.12)
        );

        assertThat(passageIds.getValue()).containsExactly(passageId.toString());
        assertThat(conceptNames.getValue()).containsExactly("Mind");
    }

    @Test
    @DisplayName("maps scored passages and deduplicated activated triples while preserving triple order")
    void mapsAndDeduplicatesResultsInOrder() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID outsideId = UUID.randomUUID();
        when(executor.runPPRPassageScores(anyString(), anyList(), anyList(), anyInt(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(
                        new PassageScoringResult(firstId.toString(), 0.91),
                        new PassageScoringResult(secondId.toString(), 0.72)
                ));
        when(executor.runPPRActivatedTriples(anyString(), anyList(), anyList(), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of(
                        row("Second", "relates_to", "Node", secondId.toString(), 0.72, 0.4),
                        row("First", "relates_to", "Node", firstId.toString(), 0.91, 0.8),
                        row("Second", "relates_to", "Node", secondId.toString(), 0.72, 0.4),
                        row("Outside", "relates_to", "Node", outsideId.toString(), 0.99, 1.0)
                ));

        GraphSearchResult result = adapter.expandKnowledge(
                GraphSearchRequest.from(List.of(GraphSeed.passage(firstId, 0.9)), 20));

        assertThat(result.scoredPassages())
                .extracting(scoredPassage -> scoredPassage.chunkId())
                .containsExactly(firstId, secondId);
        assertThat(result.activatedTriples())
                .extracting(triple -> triple.subject().name())
                .containsExactly("Second", "First");
        assertThat(result.activatedTriples())
                .extracting(triple -> triple.passage().chunkId())
                .containsExactly(secondId, firstId);
    }

    @Test
    @DisplayName("drops the projection without masking a PPR failure")
    void dropFailureDoesNotMaskPprFailure() {
        RuntimeException pprFailure = new RuntimeException("PPR failed");
        RuntimeException dropFailure = new RuntimeException("drop failed");
        when(executor.runPPRPassageScores(anyString(), anyList(), anyList(), anyInt(), anyDouble(), anyDouble(), anyInt()))
                .thenThrow(pprFailure);
        doThrow(dropFailure).when(executor).dropProjectedGraph(anyString());

        assertThatThrownBy(() -> adapter.expandKnowledge(
                GraphSearchRequest.from(List.of(GraphSeed.passage(UUID.randomUUID(), 0.9)), 20)))
                .isSameAs(pprFailure);

        verify(executor).dropProjectedGraph(anyString());
    }

    @Test
    @DisplayName("does not fail a successful search when graph cleanup fails")
    void dropFailureDoesNotBreakSuccessfulSearch() {
        UUID chunkId = UUID.randomUUID();
        when(executor.runPPRPassageScores(anyString(), anyList(), anyList(), anyInt(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(new PassageScoringResult(chunkId.toString(), 0.7)));
        when(executor.runPPRActivatedTriples(anyString(), anyList(), anyList(), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of(row("A", "rel", "B", chunkId.toString(), 0.7, 1.0)));
        doThrow(new RuntimeException("drop failed")).when(executor).dropProjectedGraph(anyString());

        assertThatCode(() -> adapter.expandKnowledge(
                GraphSearchRequest.from(List.of(GraphSeed.passage(chunkId, 0.9)), 20)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cleanupOrphanProjections delegates and swallows executor failures")
    void cleanupOrphanProjectionsDelegatesAndSwallowsFailures() {
        when(executor.dropOrphanProjections()).thenReturn(List.of("hipporag-a"));
        adapter.cleanupOrphanProjections();
        verify(executor).dropOrphanProjections();

        doThrow(new RuntimeException("Neo4j down")).when(executor).dropOrphanProjections();

        assertThatCode(() -> adapter.cleanupOrphanProjections()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("does not run activated triples query when passage score query fails")
    void pprScoreFailureStopsActivatedTriplesQuery() {
        when(executor.runPPRPassageScores(anyString(), anyList(), anyList(), anyInt(), anyDouble(), anyDouble(), anyInt()))
                .thenThrow(new RuntimeException("score query failed"));

        assertThatThrownBy(() -> adapter.expandKnowledge(
                GraphSearchRequest.from(List.of(GraphSeed.passage(UUID.randomUUID(), 0.9)), 20)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("score query failed");

        verify(executor, never()).runPPRActivatedTriples(anyString(), anyList(), anyList(), anyInt(), anyDouble(), anyDouble());
    }

    private GraphExpansionResult row(
            String subject,
            String predicate,
            String object,
            String chunkId,
            double score,
            double weight
    ) {
        return new GraphExpansionResult() {
            @Override
            public String subject() {
                return subject;
            }

            @Override
            public String predicate() {
                return predicate;
            }

            @Override
            public String object() {
                return object;
            }

            @Override
            public String chunkId() {
                return chunkId;
            }

            @Override
            public double score() {
                return score;
            }

            @Override
            public double weight() {
                return weight;
            }
        };
    }
}
