package com.kairos.context_engine.infrastructure.relational.repository.triple;

import com.kairos.context_engine.infrastructure.relational.entity.TripleEntity;
import com.kairos.context_engine.infrastructure.relational.projection.ConceptCandidateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaTripleRepository extends JpaRepository<TripleEntity, String> {


    /**
     * Executes a native pgvector cosine similarity search across all unique concepts (subjects and objects) in the triples table.
     * The query uses a Common Table Expression (CTE) to combine distinct subjects and objects into a single set of concepts, each with its associated embedding.
     * It then calculates the cosine similarity between the query vector and each concept's embedding, returning the top-k concepts ordered by similarity.
     * @param queryVector The dense embedding representation of the query concept.
     * @param limit The maximum number of candidate concepts to return.
     * @return A list of concept candidates with their name and similarity score, ordered by descending similarity.
     */
    @Query(
            value = """
                    WITH concepts AS (
                        SELECT DISTINCT subject AS concept, embedding
                        FROM triples
                        UNION ALL
                        SELECT DISTINCT object AS concept, embedding
                        FROM triples
                    )
                    SELECT
                        c.concept AS name,
                        MAX(1 - (c.embedding <=> cast(:queryVector AS vector))) AS similarity
                    FROM concepts c
                    GROUP BY c.concept
                    ORDER BY MAX(1 - (c.embedding <=> cast(:queryVector AS vector))) DESC
                    LIMIT :limit
                    """
    )
    List<ConceptCandidateProjection> findCandidates(
            @Param("queryVector") float[] queryVector,
            @Param("limit") int limit
    );
    
}
