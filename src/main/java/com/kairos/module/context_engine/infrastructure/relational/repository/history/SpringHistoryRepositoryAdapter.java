package com.kairos.module.context_engine.infrastructure.relational.repository.history;

import com.kairos.module.context_engine.domain.model.history.Answer;
import com.kairos.module.context_engine.domain.model.history.Question;
import com.kairos.module.context_engine.domain.port.repository.HistoryRepository;
import com.kairos.module.context_engine.infrastructure.relational.entity.AnswerEntity;
import com.kairos.module.context_engine.infrastructure.relational.entity.QuestionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SpringHistoryRepositoryAdapter implements HistoryRepository {
    private final JpaQuestionRepository questions;
    private final JpaAnswerRepository answers;
    public SpringHistoryRepositoryAdapter(JpaQuestionRepository questions, JpaAnswerRepository answers) { this.questions = questions; this.answers = answers; }
    public void saveQuestion(Question question) { questions.save(QuestionEntity.of(question)); }
    public void saveAnswer(Answer answer) { answers.save(AnswerEntity.of(answer)); }
    public Optional<Question> findQuestionByIdAndUserId(UUID questionId, UUID userId) { return questions.findByIdAndUserId(questionId, userId).map(QuestionEntity::toDomain); }
    public Optional<Answer> findAnswerByIdAndUserId(UUID answerId, UUID userId) { return answers.findByIdAndUserId(answerId, userId).map(AnswerEntity::toDomain); }
    public List<Answer> findAnswersByQuestionIdAndUserId(UUID questionId, UUID userId) { return answers.findAllByQuestionIdAndUserId(questionId, userId).stream().map(AnswerEntity::toDomain).toList(); }
}
