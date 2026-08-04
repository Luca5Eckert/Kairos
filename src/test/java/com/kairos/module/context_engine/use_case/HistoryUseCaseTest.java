package com.kairos.module.context_engine.use_case;

import com.kairos.module.context_engine.application.use_case.ListHistoryAnswersUseCase;
import com.kairos.module.context_engine.application.use_case.ListHistoryQuestionsUseCase;

import com.kairos.module.context_engine.domain.model.history.Answer;
import com.kairos.module.context_engine.domain.model.history.AnswerSnapshot;
import com.kairos.module.context_engine.domain.model.history.HistoryPage;
import com.kairos.module.context_engine.domain.model.history.HistoryPageRequest;
import com.kairos.module.context_engine.domain.model.history.Question;
import com.kairos.module.context_engine.domain.model.history.QuestionHistory;
import com.kairos.module.context_engine.domain.port.repository.HistoryRepository;
import com.kairos.module.context_engine.infrastructure.config.HistoryProperties;
import com.kairos.share.security.context.RequestContext;
import com.kairos.share.security.context.RequestContextProvider;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryUseCaseTest {
    @Mock private HistoryRepository historyRepository;
    @Mock private RequestContextProvider requestContextProvider;

    @Test
    void listsQuestionsForTheAuthenticatedUserAndRejectsOversizedPages() {
        UUID userId = UUID.randomUUID();
        when(requestContextProvider.getRequestContext()).thenReturn(new RequestContext(userId, "user@example.com", List.of()));
        var useCase = new ListHistoryQuestionsUseCase(historyRepository, requestContextProvider, new HistoryProperties(20, 50));
        when(historyRepository.findQuestionsByUserId(userId, new HistoryPageRequest(1, 20)))
                .thenReturn(new HistoryPage<>(List.of(), 1, 20, 0));

        useCase.execute(1, 20);
        verify(historyRepository).findQuestionsByUserId(userId, new HistoryPageRequest(1, 20));
        assertThatThrownBy(() -> useCase.execute(0, 51)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotListAnswersForAQuestionOwnedByAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        when(requestContextProvider.getRequestContext()).thenReturn(new RequestContext(userId, "user@example.com", List.of()));
        when(historyRepository.findQuestionByIdAndUserId(questionId, userId)).thenReturn(java.util.Optional.empty());
        var useCase = new ListHistoryAnswersUseCase(historyRepository, requestContextProvider, new HistoryProperties(20, 50));

        assertThatThrownBy(() -> useCase.execute(questionId, 0, 20)).isInstanceOf(EntityNotFoundException.class);
    }
}
