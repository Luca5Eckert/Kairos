package com.kairos.context_engine.application.use_case;

import com.kairos.context_engine.application.command.UploadSourceCommand;
import com.kairos.context_engine.domain.event.CreatedSourceEvent;
import com.kairos.context_engine.domain.model.Source;
import com.kairos.context_engine.domain.event.SourceEventPublisher;
import com.kairos.context_engine.domain.port.SourceRepository;
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
    private final SourceEventPublisher eventPublisher;

    public UploadSourceUseCase(SourceRepository sourceRepository, SourceEventPublisher eventPublisher) {
        this.sourceRepository = sourceRepository;
        this.eventPublisher = eventPublisher;
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

        var createdEvent = CreatedSourceEvent.of(source.getId(), source.getContent());
        eventPublisher.send(createdEvent);

        return source.getId();
    }

}