package com.kairos.application.use_case;

import com.kairos.application.command.UploadSourceCommand;
import com.kairos.domain.event.CreatedSourceEvent;
import com.kairos.domain.model.Source;
import com.kairos.domain.event.SourceEventPublisher;
import com.kairos.domain.port.SourceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class UploadSourceUseCase {

    private final SourceRepository sourceRepository;
    private final SourceEventPublisher eventPublisher;

    public UploadSourceUseCase(SourceRepository sourceRepository, SourceEventPublisher eventPublisher) {
        this.sourceRepository = sourceRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UUID execute(UploadSourceCommand command) {
        var source = Source.create(command.title(), command.content());

        sourceRepository.save(source);

        var createdEvent = CreatedSourceEvent.of(source.getId(), source.getContent());
        eventPublisher.send(createdEvent);

        return source.getId();
    }

}