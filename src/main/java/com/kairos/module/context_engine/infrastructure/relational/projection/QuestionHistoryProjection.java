package com.kairos.module.context_engine.infrastructure.relational.projection;

import java.time.Instant;
import java.util.UUID;

public interface QuestionHistoryProjection {
    UUID getId();
    UUID getUserId();
    String getText();
    Instant getCreatedAt();
    Long getAnswerCount();
    Instant getLatestAnswerAt();
}
