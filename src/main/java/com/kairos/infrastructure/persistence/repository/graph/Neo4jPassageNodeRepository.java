package com.kairos.infrastructure.persistence.repository.graph;

import com.kairos.infrastructure.persistence.entity.graph.PassageNode;
import com.kairos.infrastructure.persistence.repository.graph.projection.GraphExpansionResult;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data Neo4j repository for {@link PassageNode} entities.
 *
 * <p>Exposes three GDS lifecycle methods ({@link #projectPhraseGraph},
 * {@link #runPPRExpansion}, {@link #dropProjectedGraph}) that <strong>must</strong>
 * be orchestrated by the caller inside a {@code try/finally} block.
 * GDS in-memory projections are not bound to Neo4j transactions, so a rollback
 * will not remove a projection that was already created — only an explicit drop will.
 *
 * <p>Convenience methods ({@link #mergePassageNode}, {@link #mergeConceptLink})
 * are used during graph ingestion and follow standard Neo4j merge semantics.
 */
@Repository
public interface Neo4jPassageNodeRepository extends Neo4jRepository<PassageNode, UUID> {

    // ── Ingestion ─────────────────────────────────────────────────────────────

    /**
     * Ensures a {@code Passage} node with the given {@code chunkId} exists,
     * creating it if absent (idempotent).
     *
     * @param chunkId stable identifier of the document chunk
     */
    @Query("""
            MERGE (:Passage {chunkId: $chunkId})
            """)
    void mergePassageNode(@Param("chunkId") UUID chunkId);

    /**
     * Creates a {@code CONTAINS} relationship between a {@code Passage} node and
     * a {@code PhraseNode}, merging both endpoints if they already exist.
     *
     * @param chunkId     identifier of the source passage
     * @param conceptName name of the target phrase/concept node
     */
    @Query("""
            MATCH (p:Passage    {chunkId: $chunkId})
            MATCH (c:PhraseNode {name: $conceptName})
            MERGE (p)-[:CONTAINS]->(c)
            """)
    void mergeConceptLink(
            @Param("chunkId")     UUID chunkId,
            @Param("conceptName") String conceptName
    );

    // ── GDS lifecycle ─────────────────────────────────────────────────────────

    /**
     * Projects all {@code PhraseNode} entities and {@code TRIPLE} relationships
     * into a named GDS in-memory graph.
     *
     * <p><strong>Caller contract:</strong> every invocation of this method must be
     * paired with a {@link #dropProjectedGraph(String)} call inside a
     * {@code finally} block. GDS projections survive transaction boundaries and
     * accumulate in heap memory if not explicitly removed.
     *
     * <p>The graph name must be unique per call; use a {@code UUID} suffix to
     * prevent collisions under concurrent load.
     *
     * @param graphName unique name for the in-memory projection (e.g. {@code "hipporag-<uuid>"})
     * @return the name of the created projection as echoed by GDS
     */
    @Query("""
            CALL gds.graph.project(
                $graphName,
                'PhraseNode',
                { TRIPLE: { orientation: 'NATURAL' } }
            )
            YIELD graphName AS name
            RETURN name
            """)
    String projectPhraseGraph(@Param("graphName") String graphName);

    /**
     * Runs Personalized PageRank (PPR) over an existing GDS projection, seeded
     * from the {@code PhraseNode} entities reachable from the given anchor passages,
     * and returns the top-scored passages together with their knowledge triples.
     *
     * <p>The query performs five logical stages:
     * <ol>
     *   <li>Resolve seed nodes — {@code PhraseNode} entities contained in the
     *       anchor passages. Returns an empty result set if no seeds are found,
     *       preventing an unintentional global PageRank.</li>
     *   <li>Stream PPR scores from the projected graph.</li>
     *   <li>Lift scores to passages via {@code CONTAINS} relationships and keep
     *       the maximum score per passage.</li>
     *   <li>Limit to the top-N passages by score.</li>
     *   <li>Expand each passage to its {@code TRIPLE} relationships (OPTIONAL MATCH).
     *       Passages without triples return one row with {@code null} triple fields.</li>
     * </ol>
     *
     * <p><strong>Return cardinality:</strong> one row per triple, not per passage.
     * A passage with {@code T} triples produces {@code T} rows; a passage with no
     * triples produces one row with {@code null} subject/predicate/object.
     * Callers must handle both cases.
     *
     * <p><strong>Precondition:</strong> the projection identified by {@code graphName}
     * must already exist (created via {@link #projectPhraseGraph(String)}).
     *
     * <p><strong>UUID comparison note:</strong> {@code chunkId} is stored as a plain
     * {@code String} in Neo4j (the driver serialises {@link UUID} to its canonical
     * hyphenated form). The {@code IN} predicate compares strings directly — the
     * original {@code toString()} wrapper was redundant and risked silent mismatches
     * across driver or Neo4j version upgrades.
     *
     * @param graphName     name of the projected GDS graph to run PPR against
     * @param anchorIds     string representations of the anchor chunk UUIDs
     * @param maxIterations maximum number of PPR iterations
     * @param dampingFactor PPR damping factor (typical value: {@code 0.85})
     * @param limit         maximum number of top passages to expand into triples
     * @return scored triples from the top-ranked passages; empty if no seeds match
     */
    @Query("""
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
            """)
    List<GraphExpansionResult> runPPRExpansion(
            @Param("graphName")     String graphName,
            @Param("anchorIds")     List<String> anchorIds,
            @Param("maxIterations") int maxIterations,
            @Param("dampingFactor") double dampingFactor,
            @Param("limit")         int limit
    );

    /**
     * Drops the named GDS in-memory projection, releasing its heap allocation.
     *
     * <p>The {@code false} argument instructs GDS to return gracefully when the
     * graph does not exist, making this call safe from a {@code finally} block
     * even if {@link #projectPhraseGraph(String)} had already failed.
     *
     * @param graphName name of the projection to remove
     * @return the name of the dropped graph as reported by GDS
     */
    @Query("""
            CALL gds.graph.drop($graphName, false)
            YIELD graphName AS dropped
            RETURN dropped
            """)
    String dropProjectedGraph(@Param("graphName") String graphName);

    /**
     * Drops all GDS projections whose names start with {@code "hipporag-"}.
     *
     * <p>Intended to be invoked by a scheduled job to reclaim heap memory from
     * projections orphaned by abrupt JVM termination, pod eviction, or any other
     * failure that prevented the {@code finally} block from running.
     *
     * @return names of all projections that were successfully removed
     */
    @Query("""
            CALL gds.graph.list()
            YIELD graphName
            WHERE graphName STARTS WITH 'hipporag-'
            CALL gds.graph.drop(graphName, false) YIELD graphName AS dropped
            RETURN collect(dropped) AS cleaned
            """)
    List<String> dropOrphanProjections();
}