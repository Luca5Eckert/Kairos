package com.kairos.context_engine.application.query;

public record SearchSourceQuery(
        String searchTerm
) {
    public static SearchSourceQuery of(String searchTerm) {
        return new SearchSourceQuery(searchTerm);
    }
}
