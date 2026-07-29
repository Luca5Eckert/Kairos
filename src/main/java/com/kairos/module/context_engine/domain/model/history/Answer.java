package com.kairos.module.context_engine.domain.model.history;

import java.time.Instant;
import java.util.UUID;

public record Answer(UUID id, UUID questionId, int schemaVersion, AnswerSnapshot snapshot, Instant createdAt) {
    public Answer {
        if (id == null || questionId == null || schemaVersion <= 0 || snapshot == null || createdAt == null) {
            throw new IllegalArgumentException("Answer id, question id, schema version, snapshot and creation time are required");
        }
    }

    public static Answer create(UUID questionId, AnswerSnapshot snapshot) {
        return new Answer(UUID.randomUUID(), questionId, AnswerSnapshot.SCHEMA_VERSION, snapshot, Instant.now());
    }
}
