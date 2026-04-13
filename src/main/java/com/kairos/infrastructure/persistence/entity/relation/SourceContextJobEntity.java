package com.kairos.infrastructure.persistence.entity.relation;

import com.kairos.domain.model.SourceContextJob;
import com.kairos.domain.model.SourceContextJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_context_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SourceContextJobEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "source_id", nullable = false, unique = true)
    private UUID sourceId;

    @Column(nullable = false)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static SourceContextJobEntity of(SourceContextJob job) {
        return new SourceContextJobEntity(
                job.getId(),
                job.getSourceId(),
                job.getStatus().name(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                job.getNextAttemptAt(),
                job.getLastError(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    public SourceContextJob toDomain() {
        return new SourceContextJob(
                id,
                sourceId,
                SourceContextJobStatus.valueOf(status),
                attemptCount,
                maxAttempts,
                nextAttemptAt,
                lastError,
                createdAt,
                updatedAt
        );
    }
}
