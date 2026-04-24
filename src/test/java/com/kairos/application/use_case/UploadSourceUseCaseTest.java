package com.kairos.application.use_case;

import com.kairos.context_engine.application.command.UploadSourceCommand;
import com.kairos.context_engine.application.use_case.UploadSourceUseCase;
import com.kairos.context_engine.domain.event.CreatedSourceEvent;
import com.kairos.context_engine.domain.event.SourceEventPublisher;
import com.kairos.context_engine.domain.model.Source;
import com.kairos.context_engine.domain.port.SourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadSourceUseCaseTest {

    @Mock private SourceRepository sourceRepository;
    @Mock private SourceEventPublisher eventPublisher;

    @InjectMocks
    private UploadSourceUseCase useCase;

    @Test
    @DisplayName("execute — returns the id of the created source")
    void execute_validCommand_returnsSourceId() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());

        UUID result = useCase.execute(command);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("execute — saves source with correct title and content")
    void execute_validCommand_savesSourceWithCorrectFields() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());

        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(Source.class);
        verify(sourceRepository).save(captor.capture());

        assertThat(captor.getValue().getTitle()).isEqualTo("Clean Code");
        assertThat(captor.getValue().getContent()).isEqualTo("some content");
    }

    @Test
    @DisplayName("execute — publishes CreatedSourceEvent with matching sourceId and content")
    void execute_validCommand_publishesEventWithCorrectFields() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());

        UUID returnedId = useCase.execute(command);

        var captor = ArgumentCaptor.forClass(CreatedSourceEvent.class);
        verify(eventPublisher).send(captor.capture());

        CreatedSourceEvent event = captor.getValue();
        assertThat(event.sourceId()).isEqualTo(returnedId);
        assertThat(event.content()).isEqualTo("some content");
    }

    @Test
    @DisplayName("execute — saves source before publishing event")
    void execute_validCommand_saveHappensBeforePublish() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());

        var inOrder = inOrder(sourceRepository, eventPublisher);

        useCase.execute(command);

        inOrder.verify(sourceRepository).save(any(Source.class));
        inOrder.verify(eventPublisher).send(any(CreatedSourceEvent.class));
    }
}