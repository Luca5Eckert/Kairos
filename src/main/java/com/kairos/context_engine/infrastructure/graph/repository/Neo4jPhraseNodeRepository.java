package com.kairos.context_engine.infrastructure.graph.repository;

import com.kairos.context_engine.infrastructure.graph.entity.PhraseNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface Neo4jPhraseNodeRepository extends Neo4jRepository<PhraseNode, String> {

    /**
     * Idempotently merges subject and object nodes along with a directed TRIPLE relationship.
     * Uses chunk_id and predicate as the uniqueness key for the relationship.
     */
    @Query("""
            MERGE (s:PhraseNode {name: $subjectName})
            MERGE (o:PhraseNode {name: $objectName})
            MERGE (s)-[r:TRIPLE {predicate: $predicate, chunk_id: $chunkId}]->(o)
            SET r.weight = $weight
            """)
    void mergeTriple(
            @Param("subjectName") String subjectName,
            @Param("objectName")  String objectName,
            @Param("predicate")   String predicate,
            @Param("chunkId")     UUID chunkId,
            @Param("weight")      double weight
    );
}
