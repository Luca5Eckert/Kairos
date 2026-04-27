package com.kairos.context_engine.infrastructure.graph.neo4j;

import com.kairos.context_engine.infrastructure.graph.neo4j.KnowledgeGraphGdsExecutor;
import com.kairos.context_engine.infrastructure.graph.neo4j.repository.projection.GraphExpansionResult;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
    void projectPhraseGraph_shouldExecuteWriteQuery() {
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            callback.execute(transactionContext);
            return null;
        }).when(session).executeWrite(any());

        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);

        executor.projectPhraseGraph("hipporag-123");

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(transactionContext).run(queryCaptor.capture(), paramsCaptor.capture());
        verify(result).consume();

        assertThat(queryCaptor.getValue()).contains("CALL gds.graph.project(");
        assertThat(paramsCaptor.getValue()).containsEntry("graphName", "hipporag-123");
    }

    @Test
    void runPPRExpansion_shouldMapDriverRowsToProjection() {
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(transactionContext);
        }).when(session).executeRead(any());

        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);
        when(record.get("subject")).thenReturn(Values.value("subject"));
        when(record.get("predicate")).thenReturn(Values.value("PREDICATE"));
        when(record.get("object")).thenReturn(Values.value("object"));
        when(record.get("chunkId")).thenReturn(Values.value("550e8400-e29b-41d4-a716-446655440000"));
        when(record.get("score")).thenReturn(Values.value(0.85d));

        doAnswer(invocation -> {
            java.util.function.Function<Record, GraphExpansionResult> mapper = invocation.getArgument(0);
            return List.of(mapper.apply(record));
        }).when(result).list(any());

        List<GraphExpansionResult> rows = executor.runPPRExpansion(
                "hipporag-123",
                List.of("a", "b"),
                20,
                0.85,
                10
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().subject()).isEqualTo("subject");
        assertThat(rows.getFirst().predicate()).isEqualTo("PREDICATE");
        assertThat(rows.getFirst().object()).isEqualTo("object");
        assertThat(rows.getFirst().chunkId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(rows.getFirst().score()).isEqualTo(0.85d);
    }

    @Test
    void dropProjectedGraph_shouldExecuteWriteQuery() {
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            callback.execute(transactionContext);
            return null;
        }).when(session).executeWrite(any());

        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);

        executor.dropProjectedGraph("hipporag-123");

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(transactionContext).run(queryCaptor.capture(), anyMap());
        assertThat(queryCaptor.getValue()).contains("CALL gds.graph.drop($graphName, false)");
    }

    @Test
    void dropOrphanProjections_shouldReturnDroppedNames() {
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(transactionContext);
        }).when(session).executeWrite(any());

        when(transactionContext.run(anyString())).thenReturn(result);
        when(record.get("dropped")).thenReturn(Values.value("hipporag-123"));

        doAnswer(invocation -> {
            java.util.function.Function<Record, String> mapper = invocation.getArgument(0);
            return List.of(mapper.apply(record));
        }).when(result).list(any());

        List<String> removed = executor.dropOrphanProjections();

        assertThat(removed).containsExactly("hipporag-123");
    }
}
