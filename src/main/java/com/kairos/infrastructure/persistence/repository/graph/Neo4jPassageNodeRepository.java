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
     * Executes the HippoRAG graph expansion.
     * Starts from anchor chunks, finds connected concepts, and expands to neighboring triples.
     */
    @Query("""
            MATCH (p:Passage)-[:CONTAINS]->(seed:PhraseNode)
            WHERE toString(p.chunkId) IN $anchorIds
            WITH collect(DISTINCT seed) AS seedNodes
            
            MATCH (n:PhraseNode)-[r:TRIPLE]->(target:PhraseNode)
            WHERE n IN seedNodes OR target IN seedNodes
            WITH n, r, target
            
            MATCH (passage:Passage)-[:CONTAINS]->(n)
            
            RETURN n.name AS subject,
                   r.predicate AS predicate, 
                   target.name AS object, 
                   toString(passage.chunkId) AS chunkId
            LIMIT 50
            """)
    List<GraphExpansionResult> expandKnowledgeFromAnchors(@Param("anchorIds") List<String> anchorIds);



}