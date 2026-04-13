package com.kairos.application.use_case;

import com.kairos.application.command.GenerateSourceContextCommand;
import com.kairos.domain.embedding.EmbeddingProvider;
import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.Source;
import com.kairos.domain.model.SourceContextJob;
import com.kairos.domain.model.SourceStatus;
import com.kairos.domain.port.ChunkRepository;
import com.kairos.domain.port.SourceContextJobRepository;
import com.kairos.domain.port.SourceRepository;
import com.kairos.domain.semantic.ChunkerExtractor;
import com.kairos.infrastructure.context.config.SourceContextJobProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateSourceContextUseCaseTest {

    @Mock private ChunkerExtractor chunkerExtractor;
    @Mock private EmbeddingProvider embeddingProvider;
    @Mock private ChunkRepository chunkRepository;
    @Mock private SourceContextJobRepository sourceContextJobRepository;
    @Mock private SourceRepository sourceRepository;

    private GenerateSourceContextUseCase useCase;
    private Source source;
    private UUID sourceId;
    private Clock clock;

    @BeforeEach
    void setUp() {
        sourceId = UUID.randomUUID();
        source = new Source(sourceId, "Clean Code", "some content", SourceStatus.PENDING);
        clock = Clock.fixed(Instant.parse("2026-04-13T10:15:30Z"), ZoneOffset.UTC);
        useCase = new GenerateSourceContextUseCase(
                chunkerExtractor,
                embeddingProvider,
                chunkRepository,
                sourceContextJobRepository,
                sourceRepository,
                new SourceContextJobProperties(5, 10, Duration.ofSeconds(30), 2.0, Duration.ofMinutes(30)),
                clock
        );

        lenient().when(chunkRepository.save(any(Chunk.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("execute â€” marks source processing, persists chunks, and creates a durable job")
    void execute_validCommand_persistsChunksAndCreatesDurableJob() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(command.content(), 200, 50)).thenReturn(List.of("chunk one", "chunk two"));
        when(embeddingProvider.embed(any())).thenReturn(new float[]{0.1f, 0.2f});
        when(sourceContextJobRepository.findBySourceId(sourceId)).thenReturn(Optional.empty());

        useCase.execute(command);

        verify(chunkRepository, org.mockito.Mockito.times(2)).save(any(Chunk.class));
        verify(sourceContextJobRepository).save(any(SourceContextJob.class));
        verify(sourceRepository, atLeastOnce()).save(source);
        assertThat(source.getStatus()).isEqualTo(SourceStatus.PROCESSING);
    }

    @Test
    @DisplayName("execute â€” chunk is saved with correct source, content, index, embedding, and extraction flag")
    void execute_validCommand_chunkHasCorrectFields() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");
        var embedding = new float[]{0.5f, 0.6f};

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt())).thenReturn(List.of("only chunk"));
        when(embeddingProvider.embed("only chunk")).thenReturn(embedding);
        when(sourceContextJobRepository.findBySourceId(sourceId)).thenReturn(Optional.empty());

        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(Chunk.class);
        verify(chunkRepository).save(captor.capture());

        Chunk saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo(source);
        assertThat(saved.getContent()).isEqualTo("only chunk");
        assertThat(saved.getIndex()).isZero();
        assertThat(saved.getEmbedding()).isEqualTo(embedding);
        assertThat(saved.isTriplesExtracted()).isFalse();
    }

    @Test
    @DisplayName("execute â€” chunk index increments correctly across multiple chunks")
    void execute_multipleChunks_indexIsSequential() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt())).thenReturn(List.of("first", "second", "third"));
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});
        when(sourceContextJobRepository.findBySourceId(sourceId)).thenReturn(Optional.empty());

        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(Chunk.class);
        verify(chunkRepository, org.mockito.Mockito.times(3)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(Chunk::getIndex)
                .containsExactly(0, 1, 2);
    }

    @Test
    @DisplayName("execute â€” source not found throws RuntimeException with sourceId in message")
    void execute_sourceNotFound_throwsException() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(sourceId.toString());

        verifyNoInteractions(chunkerExtractor, embeddingProvider, chunkRepository, sourceContextJobRepository);
    }

    @Test
    @DisplayName("execute â€” no chunks produced marks source completed and skips job creation")
    void execute_noChunksProduced_marksSourceCompletedWithoutJob() {
        var command = GenerateSourceContextCommand.of(sourceId, "");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt())).thenReturn(List.of());

        useCase.execute(command);

        verifyNoInteractions(embeddingProvider, chunkRepository, sourceContextJobRepository);
        assertThat(source.getStatus()).isEqualTo(SourceStatus.COMPLETED);
    }

    @Test
    @DisplayName("execute â€” embedding is called once per chunk with the exact chunk text")
    void execute_multipleChunks_embedCalledWithCorrectText() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt())).thenReturn(List.of("alpha", "beta"));
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.0f});
        when(sourceContextJobRepository.findBySourceId(sourceId)).thenReturn(Optional.empty());

        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(String.class);
        verify(embeddingProvider, org.mockito.Mockito.times(2)).embed(captor.capture());
        assertThat(captor.getAllValues()).containsExactly("alpha", "beta");
    }

    @Test
    @DisplayName("execute â€” skips duplicate job creation when one already exists")
    void execute_existingJob_doesNotCreateAnother() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt())).thenReturn(List.of("chunk"));
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.0f});
        when(sourceContextJobRepository.findBySourceId(sourceId))
                .thenReturn(Optional.of(SourceContextJob.create(sourceId, 5, Instant.now(clock))));

        useCase.execute(command);

        verify(sourceContextJobRepository, never()).save(any(SourceContextJob.class));
    }
}
