package com.kairos.module.context_engine.infrastructure.relational.repository.history;

import com.kairos.module.context_engine.infrastructure.relational.entity.QuestionEntity;
import com.kairos.module.context_engine.infrastructure.relational.projection.QuestionHistoryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface JpaQuestionRepository extends JpaRepository<QuestionEntity, UUID> {
    Optional<QuestionEntity> findByIdAndUserId(UUID id, UUID userId);

    @Query(value = """
            select q.id as id, q.userId as userId, q.text as text, q.createdAt as createdAt,
                   count(a.id) as answerCount, max(a.createdAt) as latestAnswerAt
            from QuestionEntity q left join AnswerEntity a on a.questionId = q.id
            where q.userId = :userId
            group by q.id, q.userId, q.text, q.createdAt
            order by q.createdAt desc, q.id desc
            """,
            countQuery = "select count(q) from QuestionEntity q where q.userId = :userId")
    Page<QuestionHistoryProjection> findHistoryByUserId(UUID userId, Pageable pageable);

    @Query("""
            select q.id as id, q.userId as userId, q.text as text, q.createdAt as createdAt,
                   count(a.id) as answerCount, max(a.createdAt) as latestAnswerAt
            from QuestionEntity q left join AnswerEntity a on a.questionId = q.id
            where q.id = :questionId and q.userId = :userId
            group by q.id, q.userId, q.text, q.createdAt
            """)
    Optional<QuestionHistoryProjection> findHistoryByIdAndUserId(UUID questionId, UUID userId);
}
