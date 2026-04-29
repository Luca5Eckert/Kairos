package com.kairos.context_engine.infrastructure.graph.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("PhraseNode")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PhraseNode {

    @Id
    private String name;

    @Relationship(type = "TRIPLE", direction = Relationship.Direction.OUTGOING)
    private List<TripleRelationship> triples = new ArrayList<>();

    @Relationship(type = "SYNONYMY", direction = Relationship.Direction.OUTGOING)
    private List<PhraseNode> synonyms = new ArrayList<>();
}
