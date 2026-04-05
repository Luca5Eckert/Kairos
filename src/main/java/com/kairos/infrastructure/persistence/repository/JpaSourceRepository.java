package com.kairos.infrastructure.persistence.repository;

import com.kairos.infrastructure.persistence.entity.SourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface JpaSourceRepository extends JpaRepository<SourceEntity, UUID> {

    @Query(
            value = "SELECT * FROM sources ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :k",
            nativeQuery = true
    )
    List<SourceEntity> searchByEmbedding(float[] embedding, int k);

}