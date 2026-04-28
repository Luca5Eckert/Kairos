package com.kairos.context_engine.infrastructure.relational.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

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

    private float[] embedding;

    @ManyToOne
    private ChunkEntity chunk;


}
