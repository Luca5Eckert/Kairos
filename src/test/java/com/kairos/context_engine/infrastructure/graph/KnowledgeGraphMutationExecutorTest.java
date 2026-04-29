package com.kairos.context_engine.infrastructure.graph;

import com.kairos.context_engine.infrastructure.graph.KnowledgeGraphMutationExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.TransactionContext;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphMutationExecutorTest {

    @Mock
    private Driver neo4jDriver;

    @Mock
    private Session session;

    @Mock
    private TransactionContext transactionContext;

    @Mock
    private Result result;

    private KnowledgeGraphMutationExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new KnowledgeGraphMutationExecutor(neo4jDriver);
        when(neo4jDriver.session()).thenReturn(session);
    }

    @Test
    void mergeTriple_shouldOpenSessionAndExecuteWrite() {
        UUID chunkId = UUID.randomUUID();

        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            callback.execute(transactionContext);
            return null;
        }).when(session).executeWrite(any());

        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);

        executor.mergeTriple("Subject", "Object", "RELATES_TO", chunkId, 0.75);

        verify(neo4jDriver).session();
        verify(session).executeWrite(any(TransactionCallback.class));
        verify(session).close();
    }

    @Test
    void mergeTriple_shouldRunQueryWithCorrectParameters() {
        UUID chunkId = UUID.randomUUID();

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);

        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            callback.execute(transactionContext);
            return null;
        }).when(session).executeWrite(any());

        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);

        executor.mergeTriple("Subject", "Object", "RELATES_TO", chunkId, 0.75);

        verify(transactionContext).run(queryCaptor.capture(), paramsCaptor.capture());

        String capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery).contains("MERGE (p:Passage {chunkId: $chunkId})");
        assertThat(capturedQuery).contains("MERGE (s:PhraseNode {name: $subjectName})");
        assertThat(capturedQuery).contains("MERGE (o:PhraseNode {name: $objectName})");
        assertThat(capturedQuery).contains("MERGE (s)-[r:TRIPLE {predicate: $predicate, chunk_id: $chunkId}]->(o)");
        assertThat(capturedQuery).contains("SET r.weight = $weight");
        assertThat(capturedQuery).contains("MERGE (p)-[:CONTAINS]->(s)");
        assertThat(capturedQuery).contains("MERGE (p)-[:CONTAINS]->(o)");

        Map<String, Object> capturedParams = paramsCaptor.getValue();
        assertThat(capturedParams)
                .containsEntry("chunkId", chunkId.toString())
                .containsEntry("subjectName", "Subject")
                .containsEntry("objectName", "Object")
                .containsEntry("predicate", "RELATES_TO")
                .containsEntry("weight", 0.75);
    }

    @Test
    void mergeTriple_shouldConsumeQueryResult() {
        UUID chunkId = UUID.randomUUID();

        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            callback.execute(transactionContext);
            return null;
        }).when(session).executeWrite(any());

        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);

        executor.mergeTriple("Subject", "Object", "RELATES_TO", chunkId, 0.75);

        verify(result).consume();
    }

    @Test
    void mergeTriple_shouldConvertChunkIdToString() {
        UUID chunkId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);

        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            callback.execute(transactionContext);
            return null;
        }).when(session).executeWrite(any());

        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);

        executor.mergeTriple("Subject", "Object", "RELATES_TO", chunkId, 0.75);

        verify(transactionContext).run(anyString(), paramsCaptor.capture());

        assertThat(paramsCaptor.getValue())
                .containsEntry("chunkId", "550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void mergeTriple_shouldCloseSessionEvenWhenExceptionIsThrown() {
        UUID chunkId = UUID.randomUUID();

        when(session.executeWrite(any())).thenThrow(new RuntimeException("Neo4j connection failure"));

        assertThatThrownBy(() -> executor.mergeTriple("Subject", "Object", "RELATES_TO", chunkId, 0.75))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Neo4j connection failure");

        verify(session).close();
    }

    @Test
    void mergeTriple_shouldHandleDifferentPredicates() {
        UUID chunkId = UUID.randomUUID();

        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);

        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            callback.execute(transactionContext);
            return null;
        }).when(session).executeWrite(any());

        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);

        executor.mergeTriple("Company", "Product", "PRODUCES", chunkId, 0.75);

        verify(transactionContext).run(anyString(), paramsCaptor.capture());

        assertThat(paramsCaptor.getValue()).containsEntry("predicate", "PRODUCES");
    }

    @Test
    void mergeTriple_shouldAllowSameSubjectAndObject() {
        UUID chunkId = UUID.randomUUID();

        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);

        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            callback.execute(transactionContext);
            return null;
        }).when(session).executeWrite(any());

        when(transactionContext.run(anyString(), anyMap())).thenReturn(result);

        executor.mergeTriple("Entity", "Entity", "SELF_REFERENCES", chunkId, 0.75);

        verify(transactionContext).run(anyString(), paramsCaptor.capture());

        Map<String, Object> params = paramsCaptor.getValue();
        assertThat(params.get("subjectName")).isEqualTo(params.get("objectName"));
    }
}
