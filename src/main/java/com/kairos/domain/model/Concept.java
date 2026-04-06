package com.kairos.domain.model;

public record Concept(
        String name,
        double centrality,
        int degree
) {
}
