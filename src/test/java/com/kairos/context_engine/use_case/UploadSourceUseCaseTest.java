package com.kairos.context_engine.use_case;

import com.kairos.context_engine.application.command.UploadSourceCommand;
import com.kairos.context_engine.application.use_case.UploadSourceUseCase;
import com.kairos.context_engine.domain.event.CreatedSourceEvent;
import com.kairos.context_engine.domain.port.event.SourceEventPublisher;
import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.content.Source;
import com.kairos.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.context_engine.domain.port.repository.SourceRepository;
import com.kairos.context_engine.domain.port.extraction.ChunkerExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadSourceUseCaseTest {

    @Mock private SourceRepository sourceRepository;
    @Mock private ChunkRepository chunkRepository;
    @Mock private SourceEventPublisher eventPublisher;
    @Mock private ChunkerExtractor chunkerExtractor;

    @InjectMocks
    private UploadSourceUseCase useCase;

    @Test
    @DisplayName("execute - returns the id of the created source")
    void execute_validCommand_returnsSourceId() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());
        when(chunkerExtractor.extract("some content", 200, 50)).thenReturn(List.of());

        UUID result = useCase.execute(command);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("execute - saves source with correct title and content")
    void execute_validCommand_savesSourceWithCorrectFields() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());
        when(chunkerExtractor.extract("some content", 200, 50)).thenReturn(List.of());

        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(Source.class);
        verify(sourceRepository).save(captor.capture());

        assertThat(captor.getValue().getTitle()).isEqualTo("Clean Code");
        assertThat(captor.getValue().getContent()).isEqualTo("some content");
    }

    @Test
    @DisplayName("execute - publishes CreatedSourceEvent with matching sourceId")
    void execute_validCommand_publishesEventWithCorrectFields() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());
        when(chunkerExtractor.extract("some content", 200, 50)).thenReturn(List.of());

        UUID returnedId = useCase.execute(command);

        var captor = ArgumentCaptor.forClass(CreatedSourceEvent.class);
        verify(eventPublisher).send(captor.capture());

        CreatedSourceEvent event = captor.getValue();
        assertThat(event.sourceId()).isEqualTo(returnedId);
    }

    @Test
    @DisplayName("execute - saves source before publishing event")
    void execute_validCommand_saveHappensBeforePublish() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());
        when(chunkerExtractor.extract("some content", 200, 50)).thenReturn(List.of());

        var inOrder = inOrder(sourceRepository, eventPublisher);

        useCase.execute(command);

        inOrder.verify(sourceRepository).save(any(Source.class));
        inOrder.verify(eventPublisher).send(any(CreatedSourceEvent.class));
    }

    @Test
    @DisplayName("execute - chunks content and persists chunks before publishing event")
    void execute_validCommand_persistsChunksBeforePublishingEvent() {
        var command = new UploadSourceCommand("Clean Code", "alpha beta gamma", UUID.randomUUID());
        when(chunkerExtractor.extract("alpha beta gamma", 200, 50))
                .thenReturn(List.of("alpha beta", "beta gamma"));

        var inOrder = inOrder(sourceRepository, chunkRepository, eventPublisher);

        useCase.execute(command);

        inOrder.verify(sourceRepository).save(any(Source.class));
        inOrder.verify(chunkRepository, times(2)).save(any(Chunk.class));
        inOrder.verify(eventPublisher).send(any(CreatedSourceEvent.class));
    }

    @Test
    @DisplayName("execute - persisted chunks keep source, content and sequential indexes")
    void execute_validCommand_persistsChunksWithExpectedFields() {
        var command = new UploadSourceCommand("Clean Code", "alpha beta gamma", UUID.randomUUID());
        when(chunkerExtractor.extract("alpha beta gamma", 200, 50))
                .thenReturn(List.of("alpha beta", "beta gamma"));

        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(Chunk.class);
        verify(chunkRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(Chunk::getContent)
                .containsExactly("alpha beta", "beta gamma");
        assertThat(captor.getAllValues())
                .extracting(Chunk::getIndex)
                .containsExactly(0, 1);
        assertThat(captor.getAllValues())
                .allSatisfy(chunk -> {
                    assertThat(chunk.getSource().getTitle()).isEqualTo("Clean Code");
                    assertThat(chunk.getEmbedding()).isNull();
                    assertThat(chunk.isProcessed()).isFalse();
                });
    }
}
