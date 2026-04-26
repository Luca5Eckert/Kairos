package com.kairos.context_engine.application.use_case;

import com.kairos.context_engine.application.command.UploadSourceCommand;
import com.kairos.context_engine.domain.event.CreatedSourceEvent;
import com.kairos.context_engine.domain.model.Chunk;
import com.kairos.context_engine.domain.model.Source;
import com.kairos.context_engine.domain.event.SourceEventPublisher;
import com.kairos.context_engine.domain.port.ChunkRepository;
import com.kairos.context_engine.domain.port.SourceRepository;
import com.kairos.context_engine.domain.semantic.ChunkerExtractor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    public UploadSourceUseCase(SourceRepository sourceRepository, ChunkRepository chunkRepository, SourceEventPublisher eventPublisher, ChunkerExtractor chunkerExtractor) {
        this.sourceRepository = sourceRepository;
        this.chunkRepository = chunkRepository;
        this.eventPublisher = eventPublisher;
        this.chunkerExtractor = chunkerExtractor;
    }

    /**
     * Executes the upload source use case. It creates a new source based on the provided command,
     * @param command The command containing information to upload source.
     * @return The unique identifier of the newly created source.
     */
    @Transactional
    public UUID execute(UploadSourceCommand command) {
        var source = Source.create(command.title(), command.content());
        sourceRepository.save(source);

        var chunks = chunkerExtractor.extract(command.content(), 200, 50);

        for (int i = 0; i < chunks.size(); i++) {
            persistenceChunk(source, chunks.get(i), i);
        }

        var createdEvent = CreatedSourceEvent.of(source.getId(), source.getContent());
        eventPublisher.send(createdEvent);

        return source.getId();
    }

    private void persistenceChunk(Source source, String text, int index) {
        Chunk chunk = Chunk.create(source, text, index);
        chunkRepository.save(chunk);
    }

}