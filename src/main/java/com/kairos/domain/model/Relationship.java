package com.kairos.domain.model;

public class Relationship {

    private final Concept target; // Renomeado de targetId, pois o tipo é Concept
    private final String predicate;

    public Relationship(Concept target, String predicate) {
        this.target = target;
        this.predicate = predicate;
    }

    public Concept getTarget() {
        return target;
    }

    public String getPredicate() {
        return predicate; // Faltava o getter do predicate
    }
}