package com.kairos.domain.model;

import java.util.UUID;

public class Concept {

    private final UUID id;

    private final String name;

    private double centrality;

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

}