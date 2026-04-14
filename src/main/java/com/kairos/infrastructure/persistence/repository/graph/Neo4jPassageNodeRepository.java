package com.kairos.infrastructure.persistence.repository.graph;

import com.kairos.infrastructure.persistence.entity.graph.PassageNode;
import com.kairos.infrastructure.persistence.repository.graph.projection.GraphExpansionResult;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface Neo4jPassageNodeRepository extends Neo4jRepository<PassageNode, UUID> {

    @Query("""
            MERGE (:Passage {chunkId: $chunkId})
            """)
    void mergePassageNode(@Param("chunkId") UUID chunkId);

    /**
     * Idempotently creates a CONTAINS relationship between a PassageNode and an existing PhraseNode.
     * Uses MATCH on PhraseNode to avoid creating orphan nodes if the concept does not yet exist.
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

    /**
     * HippoRAG 2 — Personalized PageRank via Neo4j GDS.
     *
     * Fluxo:
     *   1. Projeta um subgrafo in-memory com os PhraseNodes e relacionamentos TRIPLE.
     *   2. Coleta os seed nodes (PhraseNodes conectados aos anchor chunks).
     *   3. Executa PPR a partir dos seeds, propagando relevância pelo grafo.
     *   4. Ranqueia os Passages pelo maior score PPR acumulado entre seus conceitos.
     *   5. Retorna as triplas dos Passages mais relevantes, ordenadas por score.
     *
     * O nome do grafo GDS é gerado dinamicamente com randomUUID() para evitar
     * colisões em chamadas concorrentes. O DROP ao final garante que não vaza
     * memória entre requisições.
     */
    @Query("""
            // ── Etapa 1: Projeta subgrafo in-memory ──────────────────────────────
            CALL gds.graph.project(
                'hipporag-' + randomUUID(),
                'PhraseNode',
                {
                    TRIPLE: {
                        orientation: 'NATURAL',
                        properties: ['predicate']
                    }
                },
                {
                    nodeProperties: ['centrality', 'degree']
                }
            )
            YIELD graphName

            // ── Etapa 2: Coleta seed nodes dos anchors ────────────────────────────
            WITH graphName
            MATCH (p:Passage)-[:CONTAINS]->(seed:PhraseNode)
            WHERE toString(p.chunkId) IN $anchorIds
            WITH graphName, collect(DISTINCT seed) AS seedNodes

            // ── Etapa 3: Executa PPR personalizado a partir dos seeds ─────────────
            CALL gds.pageRank.stream(graphName, {
                maxIterations:  $maxIterations,
                dampingFactor:  $dampingFactor,
                sourceNodes:    seedNodes
            })
            YIELD nodeId, score

            WITH graphName, gds.util.asNode(nodeId) AS phrase, score
            WHERE score > 0

            // ── Etapa 4: Sobe ao Passage e agrega score máximo por chunk ──────────
            MATCH (passage:Passage)-[:CONTAINS]->(phrase)
            WITH graphName, passage, max(score) AS passageScore
            ORDER BY passageScore DESC
            LIMIT $limit

            // ── Etapa 5: Retorna triplas com score ────────────────────────────────
            MATCH (passage)-[:CONTAINS]->(n:PhraseNode)-[r:TRIPLE]->(target:PhraseNode)
            WITH graphName, n, r, target, passage, passageScore

            // ── Cleanup: remove projeção da memória ───────────────────────────────
            CALL gds.graph.drop(graphName) YIELD graphName AS dropped

            RETURN n.name                      AS subject,
                   r.predicate                 AS predicate,
                   target.name                 AS object,
                   toString(passage.chunkId)   AS chunkId,
                   passageScore                AS score
            ORDER BY passageScore DESC
            """)
    List<GraphExpansionResult> expandKnowledgeFromAnchors(
            @Param("anchorIds")      List<String> anchorIds,
            @Param("maxIterations")  int maxIterations,
            @Param("dampingFactor")  double dampingFactor,
            @Param("limit")          int limit
    );

}
