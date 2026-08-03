package com.kairos.module.context_engine.presentation.dto.response;

import com.kairos.module.context_engine.domain.model.history.HistoryPage;

import java.util.List;
import java.util.function.Function;

public record HistoryPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <D, T> HistoryPageResponse<T> of(HistoryPage<D> page, Function<D, T> mapper) {
        return new HistoryPageResponse<>(page.content().stream().map(mapper).toList(), page.page(), page.size(),
                page.totalElements(), page.totalPages(), page.first(), page.last());
    }
}
