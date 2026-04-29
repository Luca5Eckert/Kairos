package com.kairos.context_engine.domain.model.knowledge;

public record Concept(
        String name
) {
    public static Concept create(String name) {
        return new Concept(name);
    }
}
