package com.kairos.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Node("PassageNode")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PassageNode {

    @Id
    private UUID chunkId;

    @Property("content")
    private String content;

    @Property("source_id")
    private UUID sourceId;

    @Relationship(type = "CONTEXT", direction = Relationship.Direction.OUTGOING)
    private List<PhraseNode> concepts = new ArrayList<>();
}
