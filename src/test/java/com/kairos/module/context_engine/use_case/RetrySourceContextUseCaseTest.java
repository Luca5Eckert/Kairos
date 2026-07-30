package com.kairos.module.context_engine.use_case;

import com.kairos.module.context_engine.application.use_case.RetrySourceContextUseCase;
import com.kairos.module.context_engine.domain.event.RetrySourceContextEvent;
import com.kairos.module.context_engine.domain.exception.SourceRetryConflictException;
import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.content.ChunkProcessingStatus;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.port.event.SourceEventPublisher;
import com.kairos.module.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.module.context_engine.domain.port.repository.SourceRepository;
import com.kairos.share.security.context.RequestContext;
import com.kairos.share.security.context.RequestContextProvider;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrySourceContextUseCaseTest {

    @Mock SourceRepository sourceRepository;
    @Mock ChunkRepository chunkRepository;
    @Mock SourceEventPublisher eventPublisher;
    @Mock RequestContextProvider requestContextProvider;
    @InjectMocks RetrySourceContextUseCase useCase;

    private UUID sourceId;
    private UUID userId;
    private Source source;

    @BeforeEach
    void setUp() {
        sourceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        source = new Source(sourceId, "Source", "content", userId);
        when(requestContextProvider.getRequestContext())
                .thenReturn(new RequestContext(userId, "user@example.com", List.of()));
    }

    @Test
    void execute_claimsOnlyFailedChunksAndPublishesRetry() {
        Chunk failed = new Chunk(UUID.randomUUID(), source, "failed", 0,
                ChunkProcessingStatus.FAILED, null);
        when(sourceRepository.findByIdAndAuthorIdForUpdate(sourceId, userId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllBySourceIdAndStatus(sourceId, ChunkProcessingStatus.PROCESSING))
                .thenReturn(List.of());
        when(chunkRepository.findAllBySourceIdAndStatus(sourceId, ChunkProcessingStatus.FAILED))
                .thenReturn(List.of(failed));

        useCase.execute(sourceId);

        assertThat(failed.getProcessingStatus()).isEqualTo(ChunkProcessingStatus.PROCESSING);
        ArgumentCaptor<RetrySourceContextEvent> event = ArgumentCaptor.forClass(RetrySourceContextEvent.class);
        verify(eventPublisher).send(event.capture());
        assertThat(event.getValue().chunkIds()).containsExactly(failed.getId());
    }

    @Test
    void execute_hidesSourcesOwnedByAnotherUser() {
        when(sourceRepository.findByIdAndAuthorIdForUpdate(sourceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(sourceId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(eventPublisher, never()).send(org.mockito.ArgumentMatchers.any(RetrySourceContextEvent.class));
    }

    @Test
    void execute_rejectsConcurrentProcessing() {
        Chunk processing = new Chunk(UUID.randomUUID(), source, "processing", 0,
                ChunkProcessingStatus.PROCESSING, null);
        when(sourceRepository.findByIdAndAuthorIdForUpdate(sourceId, userId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllBySourceIdAndStatus(sourceId, ChunkProcessingStatus.PROCESSING))
                .thenReturn(List.of(processing));

        assertThatThrownBy(() -> useCase.execute(sourceId))
                .isInstanceOf(SourceRetryConflictException.class)
                .hasMessageContaining("already in progress");
    }

    @Test
    void execute_rejectsSourceWithoutFailedChunks() {
        when(sourceRepository.findByIdAndAuthorIdForUpdate(sourceId, userId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllBySourceIdAndStatus(sourceId, ChunkProcessingStatus.PROCESSING))
                .thenReturn(List.of());
        when(chunkRepository.findAllBySourceIdAndStatus(sourceId, ChunkProcessingStatus.FAILED))
                .thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(sourceId))
                .isInstanceOf(SourceRetryConflictException.class)
                .hasMessageContaining("no failed chunks");
    }
}
