package com.kairos.module.context_engine.infrastructure.relational.repository.source;

import com.kairos.module.context_engine.infrastructure.relational.entity.SourceEntity;
import com.kairos.module.context_engine.infrastructure.relational.projection.SourceProgressProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSourceRepository extends JpaRepository<SourceEntity, UUID> {

    Optional<SourceEntity> findFirstByAuthorIdAndTitleAndContent(UUID authorId, String title, String content);

    List<SourceProgressProjection> findAllSourcesProgressByAuthorId(UUID authorId);
}
