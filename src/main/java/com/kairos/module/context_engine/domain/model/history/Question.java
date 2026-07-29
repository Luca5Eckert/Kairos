package com.kairos.module.context_engine.domain.model.history;

import java.time.Instant;
import java.util.UUID;

public record Question(UUID id, UUID userId, String text, Instant createdAt) {

    public Question {
        if (id == null || userId == null || text == null || text.isBlank() || createdAt == null) {
            throw new IllegalArgumentException("Question id, user id, text and creation time are required");
        }
        text = text.trim();
    }

    public static Question create(UUID userId, String text) {
        return new Question(UUID.randomUUID(), userId, text, Instant.now());
    }
}
