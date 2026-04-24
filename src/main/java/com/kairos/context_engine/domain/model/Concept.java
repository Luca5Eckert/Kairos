package com.kairos.context_engine.domain.model;

public record Concept(
        String name,
        double centrality,
        int degree
) {
    public static Concept create(String name) {
        return new Concept(name, 0, 0);
    }
}
