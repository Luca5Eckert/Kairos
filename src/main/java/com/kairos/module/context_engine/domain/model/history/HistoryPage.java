package com.kairos.module.context_engine.domain.model.history;

import java.util.List;

public record HistoryPage<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {
    public HistoryPage {
        if (content == null || page < 0 || size <= 0 || totalElements < 0) {
            throw new IllegalArgumentException("History page fields are invalid");
        }
        content = List.copyOf(content);
    }

    public int totalPages() {
        return totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean first() {
        return page == 0;
    }

    public boolean last() {
        return totalPages() == 0 || page >= totalPages() - 1;
    }
}
