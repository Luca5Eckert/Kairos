package com.kairos.module.context_engine.infrastructure.relational.entity;

import com.kairos.module.context_engine.domain.model.history.Answer;
import com.kairos.module.context_engine.domain.model.history.AnswerSnapshot;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "answers")
@Immutable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AnswerEntity {
    @Id private UUID id;
    @Column(name = "question_id", nullable = false) private UUID questionId;
    @Column(name = "schema_version", nullable = false) private int schemaVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private AnswerSnapshot snapshot;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public static AnswerEntity of(Answer answer) {
        return new AnswerEntity(answer.id(), answer.questionId(), answer.schemaVersion(), answer.snapshot(), answer.createdAt());
    }
    public Answer toDomain() { return new Answer(id, questionId, schemaVersion, snapshot, createdAt); }
}
