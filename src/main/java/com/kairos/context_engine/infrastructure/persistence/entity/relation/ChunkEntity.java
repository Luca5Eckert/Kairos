package com.kairos.context_engine.infrastructure.persistence.entity.relation;

import com.kairos.context_engine.domain.model.Chunk;
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

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    private float[] embedding;

    public static ChunkEntity create(Chunk chunk) {
        return new ChunkEntity(
                chunk.getId(),
                new SourceEntity(chunk.getSource().getId()),
                chunk.getContent(),
                chunk.getIndex(),
                chunk.getEmbedding()
        );
    }

    public Chunk toDomain() {
        return new Chunk(
                id,
                source.toDomain(),
                content,
                index,
                embedding
        );
    }
}