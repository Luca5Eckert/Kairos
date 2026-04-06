package com.kairos.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Concept {

    private final UUID id;
    private final String name;
    private double centrality;
    private List<Relationship> relationships = new ArrayList<>();

    public Concept(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getCentrality() {
        return centrality;
    }

    public void setCentrality(double centrality) {
        this.centrality = centrality;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<Relationship> relationships) {
        this.relationships = relationships;
    }
}