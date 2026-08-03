package com.kairos.module.context_engine.infrastructure.relational.repository.history;

import com.kairos.module.context_engine.infrastructure.relational.entity.AnswerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAnswerRepository extends JpaRepository<AnswerEntity, UUID> {
    @Query("select a from AnswerEntity a, QuestionEntity q where a.questionId = q.id and a.id = :answerId and q.userId = :userId")
    Optional<AnswerEntity> findByIdAndUserId(UUID answerId, UUID userId);

    @Query("select a from AnswerEntity a, QuestionEntity q where a.questionId = q.id and a.questionId = :questionId and q.userId = :userId order by a.createdAt")
    List<AnswerEntity> findAllByQuestionIdAndUserId(UUID questionId, UUID userId);

    @Query(value = """
            select a from AnswerEntity a, QuestionEntity q
            where a.questionId = q.id and a.questionId = :questionId and q.userId = :userId
            order by a.createdAt desc, a.id desc
            """,
            countQuery = """
            select count(a) from AnswerEntity a, QuestionEntity q
            where a.questionId = q.id and a.questionId = :questionId and q.userId = :userId
            """)
    Page<AnswerEntity> findPageByQuestionIdAndUserId(UUID questionId, UUID userId, Pageable pageable);
}
