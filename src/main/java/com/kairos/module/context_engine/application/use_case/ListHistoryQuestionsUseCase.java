package com.kairos.module.context_engine.application.use_case;

import com.kairos.module.context_engine.domain.model.history.HistoryPage;
import com.kairos.module.context_engine.domain.model.history.HistoryPageRequest;
import com.kairos.module.context_engine.domain.model.history.QuestionHistory;
import com.kairos.module.context_engine.domain.port.repository.HistoryRepository;
import com.kairos.module.context_engine.infrastructure.config.HistoryProperties;
import com.kairos.share.security.context.RequestContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListHistoryQuestionsUseCase {
    private final HistoryRepository historyRepository;
    private final RequestContextProvider requestContextProvider;
    private final HistoryProperties historyProperties;

    public HistoryPage<QuestionHistory> execute(Integer page, Integer size) {
        HistoryPageRequest pageRequest = pageRequest(page, size);
        return historyRepository.findQuestionsByUserId(
                requestContextProvider.getRequestContext().userId(), pageRequest);
    }

    private HistoryPageRequest pageRequest(Integer page, Integer size) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? historyProperties.defaultPageSize() : size;
        if (resolvedSize > historyProperties.maxPageSize()) {
            throw new IllegalArgumentException("Page size cannot exceed " + historyProperties.maxPageSize());
        }
        return new HistoryPageRequest(resolvedPage, resolvedSize);
    }
}
