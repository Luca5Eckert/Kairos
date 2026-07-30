package com.kairos.module.context_engine.infrastructure.relational.repository.source;

import com.kairos.module.context_engine.infrastructure.relational.entity.SourceEntity;
import com.kairos.module.context_engine.infrastructure.relational.projection.SourceProgressProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface JpaSourceRepository extends JpaRepository<SourceEntity, UUID> {

    Optional<SourceEntity> findFirstByAuthorIdAndTitleAndContent(UUID authorId, String title, String content);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SourceEntity> findByIdAndAuthorId(UUID id, UUID authorId);

    /**
     * Find all sources progress by author id.
     *
     * @param authorId The unique identifier of the author.
     * @return A list of source progress projections for the specified author.
     */
    @Query("""
            SELECT
                s.id AS id,
                s.title AS title,
                s.content AS content,
                s.authorId AS authorId,
                COUNT(c.id) AS totalChunks,
                COALESCE(
                    SUM(CASE WHEN c.processingStatus = com.kairos.module.context_engine.domain.model.content.ChunkProcessingStatus.PENDING THEN 1 ELSE 0 END),
                    0
                ) AS pendingChunks,
                COALESCE(
                    SUM(CASE WHEN c.processingStatus = com.kairos.module.context_engine.domain.model.content.ChunkProcessingStatus.PROCESSING THEN 1 ELSE 0 END),
                    0
                ) AS processingChunks,
                COALESCE(
                    SUM(CASE WHEN c.processingStatus = com.kairos.module.context_engine.domain.model.content.ChunkProcessingStatus.COMPLETED THEN 1 ELSE 0 END),
                    0
                ) AS completedChunks,
                COALESCE(
                    SUM(CASE WHEN c.processingStatus = com.kairos.module.context_engine.domain.model.content.ChunkProcessingStatus.FAILED THEN 1 ELSE 0 END),
                    0
                ) AS failedChunks
            FROM SourceEntity s
            LEFT JOIN s.chunkEntities c
            WHERE s.authorId = :authorId
            GROUP BY s.id, s.title, s.content, s.authorId
            """)
    List<SourceProgressProjection> findAllSourcesProgressByAuthorId(
            @Param("authorId") UUID authorId
    );

}
