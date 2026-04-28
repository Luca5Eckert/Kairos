package com.kairos.context_engine.infrastructure.relational.entity;

import com.kairos.context_engine.domain.model.content.TripleExtracted;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "triples")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TripleEntity {

    @Id
    private String key;

    private String subject;
    private String predicate;
    private String object;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    private float[] embedding;

    @ManyToOne
    private ChunkEntity chunk;

    public static TripleEntity of(TripleExtracted triple) {
        return TripleEntity.builder()
                .key(triple.getKey())
                .subject(triple.getSuject())
                .predicate(triple.getPredicate())
                .object(triple.getObject())
                .embedding(triple.getEmbedding())
                .chunk(ChunkEntity.create(triple.getChunk()))
                .build();
    }

}
