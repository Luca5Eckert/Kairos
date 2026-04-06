package com.kairos.infrastructure.persistence.entity;

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

    @TargetNode
    private PhraseNode target;
}
