package com.kairos.domain.model;

import java.time.Instant;
import java.util.UUID;

public class SourceContextJob {

    private final UUID id;
    private final UUID sourceId;
    private SourceContextJobStatus status;
    private int attemptCount;
    private final int maxAttempts;
    private Instant nextAttemptAt;
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    public SourceContextJob(
            UUID id,
            UUID sourceId,
            SourceContextJobStatus status,
            int attemptCount,
            int maxAttempts,
            Instant nextAttemptAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.sourceId = sourceId;
        this.status = status;
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SourceContextJob create(UUID sourceId, int maxAttempts, Instant now) {
        return new SourceContextJob(
                UUID.randomUUID(),
                sourceId,
                SourceContextJobStatus.PENDING,
                0,
                maxAttempts,
                now,
                null,
                now,
                now
        );
    }

    public void markProcessing(Instant now) {
        this.status = SourceContextJobStatus.PROCESSING;
        this.updatedAt = now;
    }

    public void markCompleted(Instant now) {
        this.status = SourceContextJobStatus.COMPLETED;
        this.lastError = null;
        this.updatedAt = now;
    }

    public void markFailed(String error, Instant now) {
        this.status = SourceContextJobStatus.FAILED;
        this.lastError = error;
        this.updatedAt = now;
    }

    public void scheduleRetry(String error, Instant nextAttemptAt, Instant now) {
        this.status = SourceContextJobStatus.RETRY_SCHEDULED;
        this.attemptCount++;
        this.lastError = error;
        this.nextAttemptAt = nextAttemptAt;
        this.updatedAt = now;
    }

    public boolean canRetry() {
        return attemptCount < maxAttempts;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public SourceContextJobStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
