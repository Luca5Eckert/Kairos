package com.kairos.context_engine.infrastructure.relational.repository.triple;

import com.kairos.context_engine.infrastructure.relational.entity.TripleEntity;
import com.kairos.context_engine.infrastructure.relational.projection.TripleCandidateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaTripleRepository extends JpaRepository<TripleEntity, String> {


    @Query(
            value = """
                    SELECT
                        t.key       AS key,
                        t.subject   AS subject,
                        t.predicate AS predicate,
                        t.object    AS object,
                        t.chunk_id  AS chunkId,
                        1 - (t.embedding <=> cast(:queryVector AS vector)) AS similarity
                    FROM triples t
                    WHERE t.embedding IS NOT NULL
                      AND t.chunk_id IS NOT NULL
                      AND t.subject IS NOT NULL
                      AND TRIM(t.subject) <> ''
                      AND t.predicate IS NOT NULL
                      AND TRIM(t.predicate) <> ''
                      AND t.object IS NOT NULL
                      AND TRIM(t.object) <> ''
                    ORDER BY t.embedding <=> cast(:queryVector AS vector)
                    LIMIT :limit
                    """, nativeQuery = true
    )
    List<TripleCandidateProjection> findTripleCandidates(
            @Param("queryVector") float[] queryVector,
            @Param("limit") int limit
    );
    
}
