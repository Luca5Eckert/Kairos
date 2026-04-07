package com.kairos.infrastructure.persistence.repository.graph;

import com.kairos.infrastructure.persistence.entity.graph.PassageNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}