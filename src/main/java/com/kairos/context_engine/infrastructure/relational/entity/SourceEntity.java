package com.kairos.context_engine.infrastructure.relational.entity;

import com.kairos.context_engine.domain.model.content.Source;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public SourceEntity(UUID id) {
        this.id = id;
    }

    public static SourceEntity of(Source source) {
        return new SourceEntity(
                source.getId(),
                source.getTitle(),
                source.getContent()
        );
    }

    public Source toDomain() {
        return new Source(this.id, this.title, this.content);
    }

}