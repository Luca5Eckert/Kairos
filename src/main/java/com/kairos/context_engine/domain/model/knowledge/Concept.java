package com.kairos.context_engine.domain.model.knowledge;

public record Concept(
        String name
) {
    public Concept {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Concept name cannot be null or blank");
        }

        name = name.trim();
    }

    public static Concept create(String name) {
        return new Concept(name);
    }
}
