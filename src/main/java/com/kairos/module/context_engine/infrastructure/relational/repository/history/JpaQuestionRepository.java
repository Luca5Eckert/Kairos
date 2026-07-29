package com.kairos.module.context_engine.infrastructure.relational.repository.history;

import com.kairos.module.context_engine.infrastructure.relational.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaQuestionRepository extends JpaRepository<QuestionEntity, UUID> {
    Optional<QuestionEntity> findByIdAndUserId(UUID id, UUID userId);
}
