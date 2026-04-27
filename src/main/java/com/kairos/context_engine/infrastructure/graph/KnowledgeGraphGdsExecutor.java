package com.kairos.context_engine.infrastructure.graph;

import com.kairos.context_engine.infrastructure.graph.repository.projection.GraphExpansionResult;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KnowledgeGraphGdsExecutor {

    private static final String PROJECT_PHRASE_GRAPH = """
            CALL gds.graph.project(
                $graphName,
                'PhraseNode',
                { TRIPLE: { orientation: 'NATURAL' } }
            )
            YIELD graphName AS name
            RETURN name
            """;

    private static final String RUN_PPR_EXPANSION = """
            MATCH (p:Passage)-[:CONTAINS]->(seed:PhraseNode)
            WHERE p.chunkId IN $anchorIds
            WITH collect(DISTINCT seed) AS seedNodes
            WHERE size(seedNodes) > 0

            CALL gds.pageRank.stream($graphName, {
                maxIterations: $maxIterations,
                dampingFactor: $dampingFactor,
                sourceNodes:   seedNodes
            })
            YIELD nodeId, score

            WITH gds.util.asNode(nodeId) AS phrase, score
            WHERE score > 0

            MATCH (passage:Passage)-[:CONTAINS]->(phrase)
            WITH passage, max(score) AS passageScore
            ORDER BY passageScore DESC
            LIMIT $limit

            OPTIONAL MATCH (passage)-[:CONTAINS]->(n:PhraseNode)-[r:TRIPLE]->(target:PhraseNode)

            RETURN
                n.name          AS subject,
                r.predicate     AS predicate,
                target.name     AS object,
                passage.chunkId AS chunkId,
                passageScore    AS score
            ORDER BY passageScore DESC
            """;

    private static final String DROP_PROJECTED_GRAPH = """
            CALL gds.graph.drop($graphName, false)
            YIELD graphName AS dropped
            RETURN dropped
            """;

    private static final String DROP_ORPHAN_PROJECTIONS = """
            CALL gds.graph.list()
            YIELD graphName
            WHERE graphName STARTS WITH 'hipporag-'
            CALL gds.graph.drop(graphName, false) YIELD graphName AS dropped
            RETURN dropped
            """;

    private final Driver neo4jDriver;

    public void projectPhraseGraph(String graphName) {
        runWrite(PROJECT_PHRASE_GRAPH, Map.of("graphName", graphName));
    }

    public List<GraphExpansionResult> runPPRExpansion(
            String graphName,
            List<String> anchorIds,
            int maxIterations,
            double dampingFactor,
            int limit
    ) {
        return runRead(
                RUN_PPR_EXPANSION,
                Map.of(
                        "graphName", graphName,
                        "anchorIds", anchorIds,
                        "maxIterations", maxIterations,
                        "dampingFactor", dampingFactor,
                        "limit", limit
                )
        );
    }

    public void dropProjectedGraph(String graphName) {
        runWrite(DROP_PROJECTED_GRAPH, Map.of("graphName", graphName));
    }

    public List<String> dropOrphanProjections() {
        try (var session = neo4jDriver.session()) {
            return session.executeWrite(transaction ->
                    transaction.run(DROP_ORPHAN_PROJECTIONS).list(record -> record.get("dropped").asString())
            );
        }
    }

    private void runWrite(String query, Map<String, Object> parameters) {
        try (var session = neo4jDriver.session()) {
            session.executeWrite(transaction -> {
                transaction.run(query, parameters).consume();
                return null;
            });
        }
    }

    private List<GraphExpansionResult> runRead(String query, Map<String, Object> parameters) {
        try (var session = neo4jDriver.session()) {
            return session.executeRead(transaction ->
                    transaction.run(query, parameters).list(record -> new DriverGraphExpansionResult(
                            nullableString(record.get("subject")),
                            nullableString(record.get("predicate")),
                            nullableString(record.get("object")),
                            nullableString(record.get("chunkId")),
                            record.get("score").isNull() ? 0d : record.get("score").asDouble()
                    ))
            );
        }
    }

    private String nullableString(org.neo4j.driver.Value value) {
        return value == null || value.isNull() ? null : value.asString();
    }

    private record DriverGraphExpansionResult(
            String subject,
            String predicate,
            String object,
            String chunkId,
            double score
    ) implements GraphExpansionResult {}
}
