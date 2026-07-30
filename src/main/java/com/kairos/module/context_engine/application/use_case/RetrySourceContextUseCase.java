package com.kairos.module.context_engine.application.use_case;

import com.kairos.module.context_engine.domain.event.RetrySourceContextEvent;
import com.kairos.module.context_engine.domain.exception.SourceRetryConflictException;
import com.kairos.module.context_engine.domain.model.content.ChunkProcessingStatus;
import com.kairos.module.context_engine.domain.port.event.SourceEventPublisher;
import com.kairos.module.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.module.context_engine.domain.port.repository.SourceRepository;
import com.kairos.share.security.context.RequestContextProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RetrySourceContextUseCase {

    private final SourceRepository sourceRepository;
    private final ChunkRepository chunkRepository;
    private final SourceEventPublisher eventPublisher;
    private final RequestContextProvider requestContextProvider;

    @Transactional
    public void execute(UUID sourceId) {
        UUID userId = requestContextProvider.getRequestContext().userId();
        sourceRepository.findByIdAndAuthorIdForUpdate(sourceId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Source not found"));

        if (!chunkRepository.findAllBySourceIdAndStatus(sourceId, ChunkProcessingStatus.PROCESSING).isEmpty()) {
            throw new SourceRetryConflictException("Source context processing is already in progress");
        }

        var failedChunks = chunkRepository.findAllBySourceIdAndStatus(sourceId, ChunkProcessingStatus.FAILED);
        if (failedChunks.isEmpty()) {
            throw new SourceRetryConflictException("Source has no failed chunks to retry");
        }

        failedChunks.forEach(chunk -> {
            chunk.markAsProcessing();
            chunkRepository.save(chunk);
        });

        eventPublisher.send(new RetrySourceContextEvent(
                sourceId,
                failedChunks.stream().map(chunk -> chunk.getId()).toList()
        ));
    }
}
