package com.kairos.module.context_engine.domain.model.history;

import java.time.Instant;
import java.util.UUID;

public record QuestionHistory(
        UUID id,
        UUID userId,
        String text,
        Instant createdAt,
        long answerCount,
        Instant latestAnswerAt
) {
    public QuestionHistory {
        if (id == null || userId == null || text == null || text.isBlank() || createdAt == null
                || answerCount < 0) {
            throw new IllegalArgumentException("Question history fields are invalid");
        }
        text = text.trim();
    }

    public static QuestionHistory from(Question question, long answerCount, Instant latestAnswerAt) {
        return new QuestionHistory(question.id(), question.userId(), question.text(), question.createdAt(),
                answerCount, latestAnswerAt);
    }
}
