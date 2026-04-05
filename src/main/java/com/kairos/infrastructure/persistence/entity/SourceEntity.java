package com.kairos.infrastructure.persistence.entity;

import com.kairos.domain.model.Source;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "sources")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private float[] embedding;

    public SourceEntity(String title, String content, float[] embedding) {
        this.title = title;
        this.content = content;
        this.embedding = embedding;
    }

    public static SourceEntity of(Source source) {
        return new SourceEntity(
                source.getTitle(),
                source.getContent(),
                source.getEmbedding()
        );
    }

    public Source toDomain() {
        return new Source(
                this.id,
                this.title,
                this.content,
                this.embedding
        );
    }
}
