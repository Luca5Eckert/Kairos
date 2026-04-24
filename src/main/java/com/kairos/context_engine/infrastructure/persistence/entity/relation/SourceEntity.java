package com.kairos.context_engine.infrastructure.persistence.entity.relation;

import com.kairos.context_engine.domain.model.Source;
import com.kairos.context_engine.domain.model.SourceStatus;
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
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String status;

    public SourceEntity(UUID id) {
        this.id = id;
    }

    public static SourceEntity of(Source source) {
        return new SourceEntity(
                source.getId(),
                source.getTitle(),
                source.getContent(),
                source.getStatus().name()
        );
    }

    public Source toDomain() {
        return new Source(this.id, this.title, this.content, SourceStatus.valueOf(this.status));
    }

}