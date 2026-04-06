package com.kairos.infrastructure.persistence.entity;

import com.kairos.domain.model.Concept;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Node("Concept")
public class ConceptNode {

    @Id
    private UUID id;

    @Property("name")
    private String name;

    @Property("centrality")
    private double centrality;

    @Relationship(type = "RELATED_TO", direction = Relationship.Direction.OUTGOING)
    private List<RelationshipEntity> relationships = new ArrayList<>();

    protected ConceptNode() {
    }

    public ConceptNode(UUID id, String name, double centrality, List<RelationshipEntity> relationships) {
        this.id = id;
        this.name = name;
        this.centrality = centrality;
        this.relationships = relationships;
    }

    public static ConceptNode of(Concept concept) {
        List<RelationshipEntity> relEntities = concept.getRelationships() != null
                ? concept.getRelationships().stream()
                .map(RelationshipEntity::of)
                .collect(Collectors.toList())
                : new ArrayList<>();

        return new ConceptNode(
                concept.getId(),
                concept.getName(),
                concept.getCentrality(),
                relEntities
        );
    }

    public Concept toDomain() {
        Concept concept = new Concept(this.id, this.name);
        concept.setCentrality(this.centrality);

        if (this.relationships != null) {
            concept.setRelationships(this.relationships.stream()
                    .map(RelationshipEntity::toDomain)
                    .collect(Collectors.toList()));
        }

        return concept;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCentrality() {
        return centrality;
    }

    public void setCentrality(double centrality) {
        this.centrality = centrality;
    }

    public List<RelationshipEntity> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<RelationshipEntity> relationships) {
        this.relationships = relationships;
    }

}