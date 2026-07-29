package com.kairos.module.context_engine.application.query;

import java.util.UUID;

public record SearchSourceQuery(
        String searchTerm,
        UUID questionId
) {
    public SearchSourceQuery(String searchTerm) {
        this(searchTerm, null);
    }

    public static SearchSourceQuery of(String searchTerm) {
        return new SearchSourceQuery(searchTerm, null);
    }
}
