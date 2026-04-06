package com.kairos.infrastructure.persistence.entity;

import com.kairos.domain.model.Concept;
import com.kairos.domain.model.Relationship;
import org.springframework.data.neo4j.core.schema.*;

@RelationshipProperties
public class RelationshipEntity {

    @RelationshipId
    private Long id;

    @Property("predicate")
    private String predicate;

    @TargetNode
    private ConceptNode target;

    public RelationshipEntity() {
    }

    public RelationshipEntity(Long id, String predicate, ConceptNode target) {
        this.id = id;
        this.predicate = predicate;
        this.target = target;
    }

    public static RelationshipEntity of(com.kairos.domain.model.Relationship relationship) {
        ConceptNode targetNode = new ConceptNode(
                relationship.getTarget().getId(),
                relationship.getTarget().getName(),
                relationship.getTarget().getCentrality(),
                new java.util.ArrayList<>()
        );

        return new RelationshipEntity(
                null,
                relationship.getPredicate(),
                targetNode
        );
    }

    public Relationship toDomain() {
        Concept targetConcept = new com.kairos.domain.model.Concept(this.target.getId(), this.target.getName());
        targetConcept.setCentrality(this.target.getCentrality());

        return new Relationship(targetConcept, this.predicate);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public ConceptNode getTarget() {
        return target;
    }

    public void setTarget(ConceptNode target) {
        this.target = target;
    }
}