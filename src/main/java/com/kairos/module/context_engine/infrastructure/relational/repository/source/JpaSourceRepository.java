package com.kairos.module.context_engine.infrastructure.relational.repository.source;

import com.kairos.module.context_engine.infrastructure.relational.entity.SourceEntity;
import com.kairos.module.context_engine.infrastructure.relational.projection.SourceProgressProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSourceRepository extends JpaRepository<SourceEntity, UUID> {

    Optional<SourceEntity> findFirstByAuthorIdAndTitleAndContent(UUID authorId, String title, String content);

    /**
     * Find all sources progress by author id.
     *
     * @param authorId The unique identifier of the author.
     * @return A list of source progress projections for the specified author.
     */
    @Query("""
            SELECT
                s.title AS title,
                s.content AS content,
                COUNT(c.id) AS totalChunks,
                COALESCE(
                    SUM(CASE WHEN c.processed = true THEN 1 ELSE 0 END),
                    0
                ) AS processedChunks
            FROM SourceEntity s
            LEFT JOIN s.chunkEntities c
            WHERE s.authorId = :authorId
            GROUP BY s.id, s.title, s.content
            """)
    List<SourceProgressProjection> findAllSourcesProgressByAuthorId(
            @Param("authorId") UUID authorId
    );

}
