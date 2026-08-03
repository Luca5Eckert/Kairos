package com.kairos.module.context_engine.domain.model.history;

public record HistoryPageRequest(int page, int size) {
    public HistoryPageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to zero");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
    }
}
