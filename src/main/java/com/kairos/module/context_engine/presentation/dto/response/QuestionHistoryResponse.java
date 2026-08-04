package com.kairos.module.context_engine.presentation.dto.response;

import com.kairos.module.context_engine.domain.model.history.QuestionHistory;

import java.time.Instant;
import java.util.UUID;

public record QuestionHistoryResponse(
        UUID questionId,
        String text,
        Instant createdAt,
        long answerCount,
        Instant latestExecutionAt
) {
    public static QuestionHistoryResponse of(QuestionHistory question) {
        return new QuestionHistoryResponse(question.id(), question.text(), question.createdAt(),
                question.answerCount(), question.latestAnswerAt());
    }
}
