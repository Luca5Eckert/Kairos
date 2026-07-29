package com.kairos.module.context_engine.infrastructure.relational.entity;

import com.kairos.module.context_engine.domain.model.history.Question;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class QuestionEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, columnDefinition = "TEXT") private String text;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public static QuestionEntity of(Question question) {
        return new QuestionEntity(question.id(), question.userId(), question.text(), question.createdAt());
    }
    public Question toDomain() { return new Question(id, userId, text, createdAt); }
}
