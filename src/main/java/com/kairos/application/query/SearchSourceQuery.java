package com.kairos.application.query;

public record SearchSourceQuery(
        String searchTerm
) {
    public static SearchSourceQuery of(String searchTerm) {
        return new SearchSourceQuery(searchTerm);
    }
}
