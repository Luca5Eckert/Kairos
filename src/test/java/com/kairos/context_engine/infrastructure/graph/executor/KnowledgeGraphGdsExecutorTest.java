package com.kairos.context_engine.infrastructure.graph.executor;

import com.kairos.context_engine.infrastructure.graph.repository.projection.GraphExpansionResult;
import com.kairos.context_engine.infrastructure.graph.repository.projection.PassageScoringResult;
import com.kairos.context_engine.infrastructure.graph.executor.KnowledgeGraphGdsExecutor.WeightedConceptSeed;
import com.kairos.context_engine.infrastructure.graph.executor.KnowledgeGraphGdsExecutor.WeightedPassageSeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.Values;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeGraphGdsExecutor")
class KnowledgeGraphGdsExecutorTest {

    @Mock
    private Driver neo4jDriver;

    @Mock
    private Session session;

    @Mock
    private TransactionContext transactionContext;

    @Mock
    private Result result;

    @Mock
    private Record record;

    private KnowledgeGraphGdsExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new KnowledgeGraphGdsExecutor(neo4jDriver);
        when(neo4jDriver.session()).thenReturn(session);
    }

    @Test
    @DisplayName("projectKnowledgeGraph executes the expected GDS projection query")
    void projectKnowledgeGraphExecutesProjectionQuery() {
        executeWriteCallback();
        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);

        executor.projectKnowledgeGraph("hipporag-123");

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(transactionContext).run(queryCaptor.capture(), paramsCaptor.capture());

        assertThat(queryCaptor.getValue()).contains("CALL gds.graph.project(");
        assertThat(queryCaptor.getValue()).contains("['PhraseNode', 'Passage']");
        assertThat(queryCaptor.getValue()).contains("properties:");
        assertThat(queryCaptor.getValue()).contains("weight:");
        assertThat(queryCaptor.getValue()).contains("defaultValue: 1.0");
        assertThat(paramsCaptor.getValue()).containsEntry("graphName", "hipporag-123");
    }

    @Test
    @DisplayName("runPPRPassageScores passes parameters and maps passage scoring rows")
    void runPPRPassageScoresPassesParamsAndMapsRows() {
        executeReadCallback();
        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);
        when(record.get("chunkId")).thenReturn(Values.value("550e8400-e29b-41d4-a716-446655440000"));
        when(record.get("score")).thenReturn(Values.value(0.85d));
        mapResultRows(record);

        List<PassageScoringResult> rows = executor.runPPRPassageScores(
                "hipporag-123",
                List.of(new WeightedPassageSeed("passage-1", 0.91)),
                List.of(new WeightedConceptSeed("Concept", 0.8)),
                20,
                0.85,
                0.001,
                10
        );

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(transactionContext).run(queryCaptor.capture(), paramsCaptor.capture());

        assertThat(queryCaptor.getValue()).contains("MATCH (passage:Passage)-[:CONTAINS]->(phrase)");
        assertThat(queryCaptor.getValue()).contains("collect([node, seed.weight])");
        assertThat(queryCaptor.getValue()).contains("sourceNodes: sourceNodes");
        assertThat(queryCaptor.getValue()).contains("relationshipWeightProperty: 'weight'");
        assertThat(queryCaptor.getValue()).contains("LIMIT $limit");
        assertThat(paramsCaptor.getValue())
                .containsEntry("graphName", "hipporag-123")
                .containsEntry("passageSeeds", List.of(Map.of("chunkId", "passage-1", "weight", 0.91)))
                .containsEntry("conceptSeeds", List.of(Map.of("name", "Concept", "weight", 0.8)))
                .containsEntry("maxIterations", 20L)
                .containsEntry("dampingFactor", 0.85)
                .containsEntry("scoreThreshold", 0.001)
                .containsEntry("limit", 10L);
        assertThat(rows)
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.chunkId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
                    assertThat(row.score()).isEqualTo(0.85d);
                });
    }

    @Test
    @DisplayName("runPPRActivatedTriples passes parameters and maps graph expansion rows")
    void runPPRActivatedTriplesPassesParamsAndMapsRows() {
        executeReadCallback();
        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);
        when(record.get("subject")).thenReturn(Values.value("Paris"));
        when(record.get("predicate")).thenReturn(Values.value("CAPITAL_OF"));
        when(record.get("object")).thenReturn(Values.value("France"));
        when(record.get("chunkId")).thenReturn(Values.value("550e8400-e29b-41d4-a716-446655440000"));
        when(record.get("score")).thenReturn(Values.value(0.65d));
        when(record.get("weight")).thenReturn(Values.value(0.42d));
        mapResultRows(record);

        List<GraphExpansionResult> rows = executor.runPPRActivatedTriples(
                "hipporag-123",
                List.of(new WeightedPassageSeed("passage-1", 0.91)),
                List.of(new WeightedConceptSeed("Concept", 0.8)),
                15,
                0.9,
                0.01
        );

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(transactionContext).run(queryCaptor.capture(), paramsCaptor.capture());

        assertThat(queryCaptor.getValue()).contains("MATCH (phrase)-[r:TRIPLE]->(target:PhraseNode)");
        assertThat(queryCaptor.getValue()).contains("collect([node, seed.weight])");
        assertThat(queryCaptor.getValue()).contains("sourceNodes: sourceNodes");
        assertThat(queryCaptor.getValue()).contains("relationshipWeightProperty: 'weight'");
        assertThat(queryCaptor.getValue()).contains("r.chunk_id              AS chunkId");
        assertThat(queryCaptor.getValue()).doesNotContain("LIMIT $limit");
        assertThat(paramsCaptor.getValue())
                .containsEntry("graphName", "hipporag-123")
                .containsEntry("passageSeeds", List.of(Map.of("chunkId", "passage-1", "weight", 0.91)))
                .containsEntry("conceptSeeds", List.of(Map.of("name", "Concept", "weight", 0.8)))
                .containsEntry("maxIterations", 15L)
                .containsEntry("dampingFactor", 0.9)
                .containsEntry("scoreThreshold", 0.01);
        assertThat(rows)
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.subject()).isEqualTo("Paris");
                    assertThat(row.predicate()).isEqualTo("CAPITAL_OF");
                    assertThat(row.object()).isEqualTo("France");
                    assertThat(row.chunkId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
                    assertThat(row.score()).isEqualTo(0.65d);
                    assertThat(row.weight()).isEqualTo(0.42d);
                });
    }

    @Test
    @DisplayName("dropOrphanProjections returns dropped graph names")
    void dropOrphanProjectionsReturnsDroppedGraphNames() {
        executeWriteCallback();
        when(transactionContext.run(anyString())).thenReturn(result);
        when(record.get("dropped")).thenReturn(Values.value("hipporag-123"));
        mapResultRows(record);

        List<String> removed = executor.dropOrphanProjections();

        assertThat(removed).containsExactly("hipporag-123");
    }

    private void executeReadCallback() {
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(transactionContext);
        }).when(session).executeRead(any());
    }

    private void executeWriteCallback() {
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(transactionContext);
        }).when(session).executeWrite(any());
    }

    @SuppressWarnings("unchecked")
    private <T> void mapResultRows(Record row) {
        doAnswer(invocation -> {
            Function<Record, T> mapper = invocation.getArgument(0);
            return List.of(mapper.apply(row));
        }).when(result).list(any(Function.class));
    }
}
