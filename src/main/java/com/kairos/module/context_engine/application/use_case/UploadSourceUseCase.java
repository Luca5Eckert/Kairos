package com.kairos.module.context_engine.application.use_case;

import com.kairos.module.context_engine.application.command.UploadSourceCommand;
import com.kairos.module.context_engine.domain.event.CreatedSourceEvent;
import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.port.event.SourceEventPublisher;
import com.kairos.module.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.module.context_engine.domain.port.repository.SourceRepository;
import com.kairos.module.context_engine.domain.port.extraction.ChunkerExtractor;
import com.kairos.share.security.context.RequestContextProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Use case for uploading a source. It creates a new source, saves it to the repository,
 * and publishes a CreatedSourceEvent.
 */
@Component
public class UploadSourceUseCase {

    private final SourceRepository sourceRepository;
    private final ChunkRepository chunkRepository;

    private final SourceEventPublisher eventPublisher;

    private final ChunkerExtractor chunkerExtractor;
    private final RequestContextProvider requestContextProvider;

    public UploadSourceUseCase(SourceRepository sourceRepository, ChunkRepository chunkRepository, SourceEventPublisher eventPublisher, ChunkerExtractor chunkerExtractor, RequestContextProvider requestContextProvider) {
        this.sourceRepository = sourceRepository;
        this.chunkRepository = chunkRepository;
        this.eventPublisher = eventPublisher;
        this.chunkerExtractor = chunkerExtractor;
        this.requestContextProvider = requestContextProvider;
    }

    /**
     * Executes the upload source use case. It creates a new source based on the provided command,
     * @param command The command containing information to upload source.
     * @return The unique identifier of the newly created source.
     */
    @Transactional
    public UUID execute(UploadSourceCommand command) {
        UUID authorId = requestContextProvider.getRequestContext().userId();
        Optional<Source> existingSource = Optional
                .ofNullable(sourceRepository.findByAuthorIdAndTitleAndContent(authorId, command.title(), command.content()))
                .orElse(Optional.empty());
        if (existingSource.isPresent()) {
            return existingSource.get().getId();
        }

        var source = Source.create(command.title(), command.content(), authorId);
        sourceRepository.save(source);

        var chunks = chunkerExtractor.extract(command.content(), 200, 50);

        for (int i = 0; i < chunks.size(); i++) {
            persistenceChunk(source, chunks.get(i), i);
        }

        var createdEvent = CreatedSourceEvent.of(source.getId());
        eventPublisher.send(createdEvent);

        return source.getId();
    }

    private void persistenceChunk(Source source, String text, int index) {
        Chunk chunk = Chunk.create(source, text, index);
        chunkRepository.save(chunk);
    }

}
