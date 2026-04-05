package com.kairos.infrastructure.persistence.repository;

import com.kairos.infrastructure.persistence.entity.SourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface JpaSourceRepository extends JpaRepository<SourceEntity, UUID> {

    @Query("FROM SourceEntity ORDER BY cosine_distance(embedding, :embedding) LIMIT :k")
    List<SourceEntity> searchByEmbedding(float[] embedding, int k);

}