package com.kairos.module.context_engine.domain.port.repository;

import com.kairos.module.context_engine.domain.model.history.Answer;
import com.kairos.module.context_engine.domain.model.history.HistoryPage;
import com.kairos.module.context_engine.domain.model.history.HistoryPageRequest;
import com.kairos.module.context_engine.domain.model.history.Question;
import com.kairos.module.context_engine.domain.model.history.QuestionHistory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistoryRepository {
    void saveQuestion(Question question);
    void saveAnswer(Answer answer);
    Optional<Question> findQuestionByIdAndUserId(UUID questionId, UUID userId);
    Optional<Answer> findAnswerByIdAndUserId(UUID answerId, UUID userId);
    List<Answer> findAnswersByQuestionIdAndUserId(UUID questionId, UUID userId);
    HistoryPage<QuestionHistory> findQuestionsByUserId(UUID userId, HistoryPageRequest pageRequest);
    Optional<QuestionHistory> findQuestionHistoryByIdAndUserId(UUID questionId, UUID userId);
    HistoryPage<Answer> findAnswersByQuestionIdAndUserId(UUID questionId, UUID userId,
                                                          HistoryPageRequest pageRequest);
}
