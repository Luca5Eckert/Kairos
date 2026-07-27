package com.kairos.context_engine.use_case;

import com.kairos.module.context_engine.application.command.UploadSourceCommand;
import com.kairos.module.context_engine.application.use_case.UploadSourceUseCase;
import com.kairos.module.context_engine.domain.event.CreatedSourceEvent;
import com.kairos.module.context_engine.domain.port.event.SourceEventPublisher;
import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.module.context_engine.domain.port.repository.SourceRepository;
import com.kairos.module.context_engine.domain.port.extraction.ChunkerExtractor;
import com.kairos.share.security.context.RequestContext;
import com.kairos.share.security.context.RequestContextProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadSourceUseCaseTest {

    @Mock private SourceRepository sourceRepository;
    @Mock private ChunkRepository chunkRepository;
    @Mock private SourceEventPublisher eventPublisher;
    @Mock private ChunkerExtractor chunkerExtractor;
    @Mock private RequestContextProvider requestContextProvider;

    @InjectMocks
    private UploadSourceUseCase useCase;

    @Test
    @DisplayName("execute - returns the id of the created source")
    void execute_validCommand_returnsSourceId() {
        givenAuthenticatedUser();
        var command = new UploadSourceCommand("Clean Code", "some content");
        when(chunkerExtractor.extract("some content", 200, 50)).thenReturn(List.of());

        UUID result = useCase.execute(command);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("execute - returns existing source id without duplicating chunks or event")
    void execute_duplicateSource_returnsExistingIdWithoutSideEffects() {
        UUID authorId = givenAuthenticatedUser();
        UUID existingId = UUID.randomUUID();
        var command = new UploadSourceCommand("Clean Code", "some content");
        when(sourceRepository.findByAuthorIdAndTitleAndContent(authorId, "Clean Code", "some content"))
                .thenReturn(Optional.of(new Source(existingId, "Clean Code", "some content", authorId)));

        UUID result = useCase.execute(command);

        assertThat(result).isEqualTo(existingId);
        verify(sourceRepository, never()).save(any(Source.class));
        verifyNoInteractions(chunkerExtractor, chunkRepository, eventPublisher);
    }

    @Test
    @DisplayName("execute - creates source when duplicate lookup returns Optional.empty")
    void execute_emptyDuplicateLookup_createsSourceAndPublishesEvent() {
        UUID authorId = givenAuthenticatedUser();
        var command = new UploadSourceCommand("Clean Code", "some content");
        when(sourceRepository.findByAuthorIdAndTitleAndContent(authorId, "Clean Code", "some content"))
                .thenReturn(Optional.empty());
        when(chunkerExtractor.extract("some content", 200, 50)).thenReturn(List.of("some content"));

        UUID result = useCase.execute(command);

        assertThat(result).isNotNull();
        verify(sourceRepository).findByAuthorIdAndTitleAndContent(authorId, "Clean Code", "some content");
        verify(sourceRepository).save(any(Source.class));
        verify(chunkRepository).save(any(Chunk.class));
        verify(eventPublisher).send(any(CreatedSourceEvent.class));
    }

    @Test
    @DisplayName("execute - treats null duplicate lookup result as absent source")
    void execute_nullDuplicateLookup_createsSourceAndPublishesEvent() {
        UUID authorId = givenAuthenticatedUser();
        var command = new UploadSourceCommand("Clean Code", "some content");
        when(sourceRepository.findByAuthorIdAndTitleAndContent(authorId, "Clean Code", "some content"))
                .thenReturn(null);
        when(chunkerExtractor.extract("some content", 200, 50)).thenReturn(List.of("some content"));

        UUID result = useCase.execute(command);

        assertThat(result).isNotNull();
        verify(sourceRepository).save(any(Source.class));
        verify(chunkRepository).save(any(Chunk.class));
        verify(eventPublisher).send(any(CreatedSourceEvent.class));
    }

    @Test
    @DisplayName("execute - saves source with correct title and content")
    void execute_validCommand_savesSourceWithCorrectFields() {
        UUID authorId = givenAuthenticatedUser();
        var command = new UploadSourceCommand("Clean Code", "some content");
        when(chunkerExtractor.extract("some content", 200, 50)).thenReturn(List.of());

        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(Source.class);
        verify(sourceRepository).save(captor.capture());

        assertThat(captor.getValue().getTitle()).isEqualTo("Clean Code");
        assertThat(captor.getValue().getContent()).isEqualTo("some content");
        assertThat(captor.getValue().getAuthorId()).isEqualTo(authorId);
    }

    @Test
    @DisplayName("execute - publishes CreatedSourceEvent with matching sourceId")
    void execute_validCommand_publishesEventWithCorrectFields() {
        givenAuthenticatedUser();
        var command = new UploadSourceCommand("Clean Code", "some content");
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
        givenAuthenticatedUser();
        var command = new UploadSourceCommand("Clean Code", "some content");
        when(chunkerExtractor.extract("some content", 200, 50)).thenReturn(List.of());

        var inOrder = inOrder(sourceRepository, eventPublisher);

        useCase.execute(command);

        inOrder.verify(sourceRepository).save(any(Source.class));
        inOrder.verify(eventPublisher).send(any(CreatedSourceEvent.class));
    }

    @Test
    @DisplayName("execute - chunks content and persists chunks before publishing event")
    void execute_validCommand_persistsChunksBeforePublishingEvent() {
        givenAuthenticatedUser();
        var command = new UploadSourceCommand("Clean Code", "alpha beta gamma");
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
        UUID authorId = givenAuthenticatedUser();
        var command = new UploadSourceCommand("Clean Code", "alpha beta gamma");
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
                    assertThat(chunk.getSource().getAuthorId()).isEqualTo(authorId);
                    assertThat(chunk.getEmbedding()).isNull();
                    assertThat(chunk.isProcessed()).isFalse();
                });
    }

    @Test
    @DisplayName("execute - propagates chunker failures without publishing event")
    void execute_chunkerFailure_doesNotPersistChunksOrPublishEvent() {
        givenAuthenticatedUser();
        var command = new UploadSourceCommand("Clean Code", "some content");
        when(chunkerExtractor.extract("some content", 200, 50))
                .thenThrow(new RuntimeException("Chunker unavailable"));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Chunker unavailable");

        verify(sourceRepository).save(any(Source.class));
        verifyNoInteractions(chunkRepository, eventPublisher);
    }

    @Test
    @DisplayName("execute - propagates publisher failures after source and chunks are saved")
    void execute_publisherFailure_happensAfterPersistence() {
        givenAuthenticatedUser();
        var command = new UploadSourceCommand("Clean Code", "alpha beta gamma");
        when(chunkerExtractor.extract("alpha beta gamma", 200, 50))
                .thenReturn(List.of("alpha beta", "beta gamma"));
        doThrow(new RuntimeException("Event bus unavailable"))
                .when(eventPublisher).send(any(CreatedSourceEvent.class));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Event bus unavailable");

        var inOrder = inOrder(sourceRepository, chunkRepository, eventPublisher);
        inOrder.verify(sourceRepository).save(any(Source.class));
        inOrder.verify(chunkRepository, times(2)).save(any(Chunk.class));
        inOrder.verify(eventPublisher).send(any(CreatedSourceEvent.class));
    }

    private UUID givenAuthenticatedUser() {
        UUID authorId = UUID.randomUUID();
        when(requestContextProvider.getRequestContext())
                .thenReturn(new RequestContext(authorId, "lucas@example.com", List.of()));
        return authorId;
    }
}
