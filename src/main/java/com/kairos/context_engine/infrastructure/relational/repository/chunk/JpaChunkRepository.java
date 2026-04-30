package com.kairos.context_engine.infrastructure.relational.repository.chunk;

import com.kairos.context_engine.infrastructure.relational.entity.ChunkEntity;
import com.kairos.context_engine.infrastructure.relational.repository.projection.PassageCandidateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link ChunkEntity} persistence and pgvector operations.
 */
@Repository
public interface JpaChunkRepository extends JpaRepository<ChunkEntity, UUID> {

    /**
     * Executes a native pgvector cosine distance search.
     *
     * @param queryVector The dense embedding representation of the query.
     * @param limit       The top-k threshold.
     * @return A list of chunks ordered by the shortest cosine distance.
     */
    @Query(value = "SELECT * FROM chunks ORDER BY embedding <=> cast(:queryVector as vector) LIMIT :limit", nativeQuery = true)
    List<ChunkEntity> findTopKByEmbedding(@Param("queryVector") float[] queryVector, @Param("limit") int limit);

    List<ChunkEntity> findAllBySource_Id(UUID sourceId);

    @Query(value = """
        SELECT
            c.id AS chunkId,
            1 - (c.embedding <=> cast(:queryVector AS vector)) AS denseScore
        FROM chunks c
        WHERE c.processed = true
        ORDER BY c.embedding <=> cast(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<PassageCandidateProjection> findCandidates(
            @Param("queryVector") float[] queryVector,
            @Param("limit") int limit
    );

}
