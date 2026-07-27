package com.kairos.module.context_engine.infrastructure.graph.executor;

import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KnowledgeGraphMutationExecutor {

    private static final String MERGE_PASSAGE = """
            MERGE (p:Passage {chunkId: $chunkId})
            SET p.user_id = $userId
            """;

    private static final String MERGE_TRIPLE_FOR_CHUNK = """
            MERGE (p:Passage {chunkId: $chunkId})
            SET p.user_id = $userId
            MERGE (s:PhraseNode {name: $subjectName})
            MERGE (o:PhraseNode {name: $objectName})
            MERGE (s)-[r:TRIPLE {predicate: $predicate, chunk_id: $chunkId, user_id: $userId}]->(o)
            SET r.weight = $weight
            MERGE (p)-[containsSubject:CONTAINS {user_id: $userId}]->(s)
            SET containsSubject.weight = 1.0
            MERGE (p)-[containsObject:CONTAINS {user_id: $userId}]->(o)
            SET containsObject.weight = 1.0
            """;

    private final Driver neo4jDriver;

    public void mergePassage(UUID chunkId, UUID userId) {
        runWrite(MERGE_PASSAGE, Map.of(
                "chunkId", chunkId.toString(),
                "userId", userId.toString()
        ));
    }

    public void mergeTriple(String subjectName, String objectName, String predicate, UUID chunkId, UUID userId, double weight) {
        runWrite(MERGE_TRIPLE_FOR_CHUNK, Map.of(
                "chunkId", chunkId.toString(),
                "userId", userId.toString(),
                "subjectName", subjectName,
                "objectName", objectName,
                "predicate", predicate,
                "weight", weight
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
