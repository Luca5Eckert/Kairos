package com.kairos.context_engine.infrastructure.graph.executor;

import com.kairos.context_engine.infrastructure.graph.repository.projection.GraphExpansionResult;
import com.kairos.context_engine.infrastructure.graph.repository.projection.PassageScoringResult;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KnowledgeGraphGdsExecutor {

    private static final String PROJECT_KNOWLEDGE_GRAPH = """
            CALL gds.graph.project(
                $graphName,
                ['PhraseNode', 'Passage'],
                {
                    TRIPLE:   { orientation: 'NATURAL' },
                    CONTAINS: { orientation: 'NATURAL' }
                }
            )
            YIELD graphName AS name
            RETURN name
            """;

    /**
     * PPR → activated PhraseNodes → join to Passage → group by passage → max(score).
     * Grouping, ordering and limiting happen inside Cypher — the caller receives
     * a ready-to-map list with no further aggregation needed.
     *
     * No triple join here: keeping the two concerns in separate queries eliminates
     * the (passages × triples) cartesian product that the combined query produced.
     */
    private static final String RUN_PPR_PASSAGE_SCORES = """
            MATCH (node)
            WHERE (node:Passage    AND node.chunkId IN $passageAnchorIds)
               OR (node:PhraseNode AND node.name    IN $conceptNames)
            WITH collect(DISTINCT node) AS seedNodes
            WHERE size(seedNodes) > 0

            CALL gds.pageRank.stream($graphName, {
                maxIterations: $maxIterations,
                dampingFactor: $dampingFactor,
                sourceNodes:   seedNodes
            })
            YIELD nodeId, score

            WITH gds.util.asNode(nodeId) AS phrase, score
            WHERE score >= $scoreThreshold AND phrase:PhraseNode

            MATCH (passage:Passage)-[:CONTAINS]->(phrase)
            WITH passage.chunkId AS chunkId, max(score) AS score
            ORDER BY score DESC
            LIMIT $limit

            RETURN chunkId, score
            """;

    /**
     * PPR → activated PhraseNodes → outgoing TRIPLE relationships.
     * The chunk the triple belongs to is read from the relationship property (r.chunk_id),
     * so no Passage join is required — zero cartesian risk.
     *
     * No LIMIT: the full activated-triple set must be preserved regardless of
     * how many passages were selected in the scoring query.
     */
    private static final String RUN_PPR_ACTIVATED_TRIPLES = """
            MATCH (node)
            WHERE (node:Passage    AND node.chunkId IN $passageAnchorIds)
               OR (node:PhraseNode AND node.name    IN $conceptNames)
            WITH collect(DISTINCT node) AS seedNodes
            WHERE size(seedNodes) > 0

            CALL gds.pageRank.stream($graphName, {
                maxIterations: $maxIterations,
                dampingFactor: $dampingFactor,
                sourceNodes:   seedNodes
            })
            YIELD nodeId, score

            WITH gds.util.asNode(nodeId) AS phrase, score
            WHERE score >= $scoreThreshold AND phrase:PhraseNode

            MATCH (phrase)-[r:TRIPLE]->(target:PhraseNode)

            RETURN
                phrase.name             AS subject,
                r.predicate             AS predicate,
                target.name             AS object,
                r.chunk_id              AS chunkId,
                score                   AS score,
                coalesce(r.weight, 1.0) AS weight
            ORDER BY score DESC
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

    public void projectKnowledgeGraph(String graphName) {
        runWrite(PROJECT_KNOWLEDGE_GRAPH, Map.of("graphName", graphName));
    }

    public List<PassageScoringResult> runPPRPassageScores(
            String graphName,
            List<String> passageAnchorIds,
            List<String> conceptNames,
            int maxIterations,
            double dampingFactor,
            double scoreThreshold,
            int limit
    ) {
        try (var session = neo4jDriver.session()) {
            return session.executeRead(transaction ->
                    transaction.run(RUN_PPR_PASSAGE_SCORES, Map.of(
                            "graphName",        graphName,
                            "passageAnchorIds", passageAnchorIds,
                            "conceptNames",     conceptNames,
                            "maxIterations",    (long) maxIterations,
                            "dampingFactor",    dampingFactor,
                            "scoreThreshold",   scoreThreshold,
                            "limit",            (long) limit
                    )).list(record -> new PassageScoringResult(
                            nullableString(record.get("chunkId")),
                            record.get("score").isNull() ? 0d : record.get("score").asDouble()
                    ))
            );
        }
    }

    public List<GraphExpansionResult> runPPRActivatedTriples(
            String graphName,
            List<String> passageAnchorIds,
            List<String> conceptNames,
            int maxIterations,
            double dampingFactor,
            double scoreThreshold
    ) {
        try (var session = neo4jDriver.session()) {
            return session.executeRead(transaction ->
                    transaction.run(RUN_PPR_ACTIVATED_TRIPLES, Map.of(
                            "graphName",        graphName,
                            "passageAnchorIds", passageAnchorIds,
                            "conceptNames",     conceptNames,
                            "maxIterations",    (long) maxIterations,
                            "dampingFactor",    dampingFactor,
                            "scoreThreshold",   scoreThreshold
                    )).list(record -> new DriverGraphExpansionResult(
                            nullableString(record.get("subject")),
                            nullableString(record.get("predicate")),
                            nullableString(record.get("object")),
                            nullableString(record.get("chunkId")),
                            record.get("score").isNull()  ? 0d : record.get("score").asDouble(),
                            record.get("weight").isNull() ? 1d : record.get("weight").asDouble()
                    ))
            );
        }
    }

    public void dropProjectedGraph(String graphName) {
        runWrite(DROP_PROJECTED_GRAPH, Map.of("graphName", graphName));
    }

    public List<String> dropOrphanProjections() {
        try (var session = neo4jDriver.session()) {
            return session.executeWrite(transaction ->
                    transaction.run(DROP_ORPHAN_PROJECTIONS)
                            .list(record -> record.get("dropped").asString())
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

    private String nullableString(org.neo4j.driver.Value value) {
        return value == null || value.isNull() ? null : value.asString();
    }

    private record DriverGraphExpansionResult(
            String subject,
            String predicate,
            String object,
            String chunkId,
            double score,
            double weight
    ) implements GraphExpansionResult {}
}