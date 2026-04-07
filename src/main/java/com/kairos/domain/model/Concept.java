package com.kairos.domain.model;

public record Concept(
        String name,
        double centrality,
        int degree
) {
    public static Concept create(Triple triple) {
        return new Concept(triple.subject(), 0, 0);
    }
}
