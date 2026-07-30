package com.kairos.module.context_engine.infrastructure.relational.entity;

import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.content.ChunkProcessingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "chunks")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChunkEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private SourceEntity source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "chunk_index", nullable = false)
    private int index;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private ChunkProcessingStatus processingStatus;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    private float[] embedding;

    public static ChunkEntity create(Chunk chunk) {
        return new ChunkEntity(
                chunk.getId(),
                new SourceEntity(chunk.getSource().getId()),
                chunk.getContent(),
                chunk.getIndex(),
                chunk.getProcessingStatus(),
                chunk.getEmbedding()
        );
    }

    public Chunk toDomain() {
        return new Chunk(
                id,
                source.toDomain(),
                content,
                index,
                processingStatus,
                embedding
        );
    }
}
