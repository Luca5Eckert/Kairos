package com.kairos.context_engine.use_case;

import com.kairos.context_engine.application.command.GenerateSourceContextCommand;
import com.kairos.context_engine.application.use_case.GenerateSourceContextUseCase;
import com.kairos.context_engine.domain.embedding.EmbeddingProvider;
import com.kairos.context_engine.domain.model.*;
import com.kairos.context_engine.domain.semantic.ChunkerExtractor;
import com.kairos.context_engine.domain.graph.TripleExtractor;
import com.kairos.context_engine.domain.port.ChunkRepository;
import com.kairos.context_engine.domain.graph.KnowledgeGraphStore;
import com.kairos.context_engine.domain.port.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateSourceContextUseCaseTest {

    @Mock private ChunkerExtractor chunkerExtractor;
    @Mock private TripleExtractor tripleExtractor;
    @Mock private EmbeddingProvider embeddingProvider;
    @Mock private KnowledgeGraphStore knowledgeGraphStore;
    @Mock private ChunkRepository chunkRepository;
    @Mock private SourceRepository sourceRepository;

    @InjectMocks
    private GenerateSourceContextUseCase useCase;

    private Source source;
    private UUID sourceId;

    @BeforeEach
    void setUp() {
        sourceId = UUID.randomUUID();
        source = new Source(sourceId, "Clean Code", "some content", SourceStatus.PENDING);
    }

    @Test
    @DisplayName("execute — processes each chunk into a saved Chunk and a set of KnowledgeTriples")
    void execute_validCommand_savesChunksAndKnowledgeTriples() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");
        var triple = new Triple("backpropagation", "USES", "chain rule");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt())).thenReturn(List.of("chunk one", "chunk two"));
        when(tripleExtractor.extract(anyString())).thenReturn(List.of(triple));
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});

        useCase.execute(command);

        verify(chunkRepository, times(2)).save(any(Chunk.class));
        verify(knowledgeGraphStore, times(2)).saveAllForChunk(any(UUID.class), anyList());
    }

    @Test
    @DisplayName("execute — chunk is saved with correct source, content, index, and embedding")
    void execute_validCommand_chunkHasCorrectFields() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");
        var embedding = new float[]{0.5f, 0.6f};

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt())).thenReturn(List.of("only chunk"));
        when(tripleExtractor.extract(anyString())).thenReturn(List.of());
        when(embeddingProvider.embed("only chunk")).thenReturn(embedding);

        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(Chunk.class);
        verify(chunkRepository).save(captor.capture());

        Chunk saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo(source);
        assertThat(saved.getContent()).isEqualTo("only chunk");
        assertThat(saved.getIndex()).isZero();
        assertThat(saved.getEmbedding()).isEqualTo(embedding);
    }

    @Test
    @DisplayName("execute — chunk index increments correctly across multiple chunks")
    void execute_multipleChunks_indexIsSequential() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt()))
                .thenReturn(List.of("first", "second", "third"));
        when(tripleExtractor.extract(anyString())).thenReturn(List.of());
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});

        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(Chunk.class);
        verify(chunkRepository, times(3)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(Chunk::getIndex)
                .containsExactly(0, 1, 2);
    }

    @Test
    @DisplayName("execute — KnowledgeTriples are linked to the correct chunkId")
    void execute_triplesExtracted_knowledgeTriplesLinkedToChunkId() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");
        var triple = new Triple("gradient descent", "MINIMIZES", "loss function");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt())).thenReturn(List.of("chunk"));
        when(tripleExtractor.extract(anyString())).thenReturn(List.of(triple));
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});

        useCase.execute(command);

        var chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(chunkRepository).save(chunkCaptor.capture());
        UUID savedChunkId = chunkCaptor.getValue().getId();

        ArgumentCaptor<List<KnowledgeTriple>> triplesCaptor = ArgumentCaptor.captor();
        verify(knowledgeGraphStore).saveAllForChunk(eq(savedChunkId), triplesCaptor.capture());

        assertThat(triplesCaptor.getValue())
                .hasSize(1)
                .allSatisfy(kt -> assertThat(kt.chunkId()).isEqualTo(savedChunkId));
    }

    @Test
    @DisplayName("execute — no triples extracted results in saveAllForChunk called with empty list")
    void execute_noTriplesExtracted_savesEmptyKnowledgeTriples() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt())).thenReturn(List.of("chunk"));
        when(tripleExtractor.extract(anyString())).thenReturn(List.of());
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});

        useCase.execute(command);

        ArgumentCaptor<List<KnowledgeTriple>> captor = ArgumentCaptor.captor();
        verify(knowledgeGraphStore).saveAllForChunk(any(UUID.class), captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("execute — source not found throws RuntimeException with sourceId in message")
    void execute_sourceNotFound_throwsException() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(sourceId.toString());

        verifyNoInteractions(chunkerExtractor, embeddingProvider, tripleExtractor,
                chunkRepository, knowledgeGraphStore);
    }

    @Test
    @DisplayName("execute — no chunks produced results in no further processing")
    void execute_noChunksProduced_noInteractionsWithDownstreamPorts() {
        var command = GenerateSourceContextCommand.of(sourceId, "");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt())).thenReturn(List.of());

        useCase.execute(command);

        verifyNoInteractions(embeddingProvider, tripleExtractor, chunkRepository, knowledgeGraphStore);
    }

    @Test
    @DisplayName("execute — embedding is called once per chunk with the exact chunk text")
    void execute_multipleChunks_embedCalledWithCorrectText() {
        var command = GenerateSourceContextCommand.of(sourceId, "some content");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkerExtractor.extract(anyString(), anyInt(), anyInt())).thenReturn(List.of("alpha", "beta"));
        when(tripleExtractor.extract(anyString())).thenReturn(List.of());
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.0f});

        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(String.class);
        verify(embeddingProvider, times(2)).embed(captor.capture());
        assertThat(captor.getAllValues()).containsExactly("alpha", "beta");
    }
}