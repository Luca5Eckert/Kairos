package com.kairos.infrastructure.persistence.entity.graph;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Node("Passage")
public class PassageNode {

    @Id
    private UUID chunkId;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<PhraseNode> concepts = new ArrayList<>();

    public static PassageNode forChunk(UUID chunkId) {
        PassageNode node = new PassageNode();
        node.setChunkId(chunkId);
        return node;
    }
}