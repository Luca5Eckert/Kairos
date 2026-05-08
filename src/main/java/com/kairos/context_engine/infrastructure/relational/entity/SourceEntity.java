package com.kairos.context_engine.infrastructure.relational.entity;

import com.kairos.context_engine.domain.model.content.Source;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
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

    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChunkEntity> chunkEntities;

    public SourceEntity(UUID id) {
        this.id = id;
    }

    public SourceEntity(UUID sourceId, String title, String content) {
        this.id = sourceId;
        this.title = title;
        this.content = content;
        this.chunkEntities = List.of();
    }

    public static SourceEntity of(Source source) {
        return new SourceEntity(
                source.getId(),
                source.getTitle(),
                source.getContent(),
                List.of()
        );
    }

    public Source toDomain() {
        return new Source(this.id, this.title, this.content);
    }

}