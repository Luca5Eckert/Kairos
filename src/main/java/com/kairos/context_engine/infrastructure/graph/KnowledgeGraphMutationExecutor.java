package com.kairos.context_engine.infrastructure.graph;

import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KnowledgeGraphMutationExecutor {

    private static final String MERGE_TRIPLE_FOR_CHUNK = """
            MERGE (p:Passage {chunkId: $chunkId})
            MERGE (s:PhraseNode {name: $subjectName})
            MERGE (o:PhraseNode {name: $objectName})
            MERGE (s)-[:TRIPLE {predicate: $predicate, chunk_id: $chunkId}]->(o)
            MERGE (p)-[:CONTAINS]->(s)
            MERGE (p)-[:CONTAINS]->(o)
            """;

    private final Driver neo4jDriver;

    public void mergeTriple(String subjectName, String objectName, String predicate, UUID chunkId) {
        runWrite(MERGE_TRIPLE_FOR_CHUNK, Map.of(
                "chunkId", chunkId.toString(),
                "subjectName", subjectName,
                "objectName", objectName,
                "predicate", predicate
        ));
    }

    private void runWrite(String query, Map<String, Object> parameters) {
        try (var session = neo4jDriver.session()) {
            session.executeWrite(transaction -> {
                transaction.run(query, parameters).consume();
                return null;
            });
        }
    }
}
