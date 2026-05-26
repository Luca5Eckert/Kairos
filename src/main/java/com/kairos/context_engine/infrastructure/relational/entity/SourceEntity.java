package com.kairos.context_engine.infrastructure.relational.entity;

import com.kairos.context_engine.domain.model.content.Source;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
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

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChunkEntity> chunkEntities;

    public SourceEntity(UUID id) {
        this.id = id;
    }

    public SourceEntity(UUID sourceId, String title, String content) {
        this.id = sourceId;
        this.title = title;
        this.content = content;
        this.authorId = null;
        this.chunkEntities = new ArrayList<>();
    }

    public SourceEntity(UUID sourceId, String title, String content, UUID authorId) {
        this.id = sourceId;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.chunkEntities = new ArrayList<>();
    }

    public static SourceEntity of(Source source) {
        return new SourceEntity(
                source.getId(),
                source.getTitle(),
                source.getContent(),
                source.getAuthorId(),
                new ArrayList<>()
        );
    }

    public Source toDomain() {
        return new Source(this.id, this.title, this.content, this.authorId);
    }

}
