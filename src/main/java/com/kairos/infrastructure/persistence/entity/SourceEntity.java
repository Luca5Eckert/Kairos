package com.kairos.infrastructure.persistence.entity;

import com.kairos.domain.model.Source;
import com.kairos.domain.model.SourceStatus;
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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String status;

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