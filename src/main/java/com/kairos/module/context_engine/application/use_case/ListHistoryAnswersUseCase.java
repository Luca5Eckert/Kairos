package com.kairos.module.context_engine.application.use_case;

import com.kairos.module.context_engine.domain.model.history.Answer;
import com.kairos.module.context_engine.domain.model.history.HistoryPage;
import com.kairos.module.context_engine.domain.model.history.HistoryPageRequest;
import com.kairos.module.context_engine.domain.port.repository.HistoryRepository;
import com.kairos.module.context_engine.infrastructure.config.HistoryProperties;
import com.kairos.share.security.context.RequestContextProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListHistoryAnswersUseCase {
    private static final String NOT_FOUND_MESSAGE = "History resource not found";

    private final HistoryRepository historyRepository;
    private final RequestContextProvider requestContextProvider;
    private final HistoryProperties historyProperties;

    public HistoryPage<Answer> execute(UUID questionId, Integer page, Integer size) {
        UUID userId = requestContextProvider.getRequestContext().userId();
        if (historyRepository.findQuestionByIdAndUserId(questionId, userId).isEmpty()) {
            throw new EntityNotFoundException(NOT_FOUND_MESSAGE);
        }

        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? historyProperties.defaultPageSize() : size;
        if (resolvedSize > historyProperties.maxPageSize()) {
            throw new IllegalArgumentException("Page size cannot exceed " + historyProperties.maxPageSize());
        }
        return historyRepository.findAnswersByQuestionIdAndUserId(questionId, userId,
                new HistoryPageRequest(resolvedPage, resolvedSize));
    }
}
