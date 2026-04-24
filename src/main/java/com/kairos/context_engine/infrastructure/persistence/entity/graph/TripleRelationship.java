package com.kairos.context_engine.infrastructure.persistence.entity.graph;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.util.UUID;

@RelationshipProperties
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TripleRelationship {

    @RelationshipId
    private Long id;

    @Property("predicate")
    private String predicate;

    @Property("chunk_id")
    private UUID chunkId;

    @Property("user_id")
    private UUID userId;

    @Property("weight")
    private double weight;

    @TargetNode
    private PhraseNode target;
}