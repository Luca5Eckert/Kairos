package com.kairos.module.context_engine.application.use_case;

import com.kairos.module.context_engine.domain.model.history.QuestionHistory;
import com.kairos.module.context_engine.domain.port.repository.HistoryRepository;
import com.kairos.share.security.context.RequestContextProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetHistoryQuestionUseCase {
    private static final String NOT_FOUND_MESSAGE = "History resource not found";

    private final HistoryRepository historyRepository;
    private final RequestContextProvider requestContextProvider;

    public QuestionHistory execute(UUID questionId) {
        UUID userId = requestContextProvider.getRequestContext().userId();
        return historyRepository.findQuestionHistoryByIdAndUserId(questionId, userId)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_MESSAGE));
    }
}
