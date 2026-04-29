package com.kairos.context_engine.use_case;

import com.kairos.context_engine.application.command.GenerateSourceContextCommand;
import com.kairos.context_engine.application.use_case.GenerateSourceContextUseCase;
import com.kairos.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.context_engine.domain.port.graph.KnowledgeGraphStore;
import com.kairos.context_engine.domain.port.extraction.TripleExtractor;
import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.context_engine.domain.model.content.Source;
import com.kairos.context_engine.domain.model.Triple;
import com.kairos.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.context_engine.domain.port.repository.SourceRepository;
import com.kairos.context_engine.domain.port.repository.TripleRepository;
import com.kairos.context_engine.domain.model.content.TripleExtracted;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateSourceContextUseCaseTest {

    @Mock private TripleExtractor tripleExtractor;
    @Mock private EmbeddingProvider embeddingProvider;
    @Mock private KnowledgeGraphStore knowledgeGraphStore;
    @Mock private ChunkRepository chunkRepository;
    @Mock private SourceRepository sourceRepository;
    @Mock private TripleRepository tripleRepository;

    @InjectMocks
    private GenerateSourceContextUseCase useCase;

    private Source source;
    private UUID sourceId;

    @BeforeEach
    void setUp() {
        sourceId = UUID.randomUUID();
        source = new Source(sourceId, "Clean Code", "some content");
    }

    private Chunk chunk(String content, int index) {
        return Chunk.create(UUID.randomUUID(), source, content, index, false, null);
    }

    @Test
    @DisplayName("execute - loads chunks already persisted for the source")
    void execute_loadsPersistedChunksForSource() {
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllBySourceId(sourceId)).thenReturn(List.of());

        useCase.execute(GenerateSourceContextCommand.of(sourceId));

        verify(chunkRepository).findAllBySourceId(sourceId);
    }

    @Test
    @DisplayName("execute - embeds each persisted chunk and saves it with the generated embedding")
    void execute_embedsAndSavesPersistedChunks() {
        Chunk first = chunk("first chunk", 0);
        Chunk second = chunk("second chunk", 1);
        float[] firstEmbedding = new float[]{0.1f, 0.2f};
        float[] secondEmbedding = new float[]{0.3f, 0.4f};

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllBySourceId(sourceId)).thenReturn(List.of(first, second));
        when(embeddingProvider.embed("first chunk")).thenReturn(firstEmbedding);
        when(embeddingProvider.embed("second chunk")).thenReturn(secondEmbedding);
        when(tripleExtractor.extract(anyString())).thenReturn(List.of());

        useCase.execute(GenerateSourceContextCommand.of(sourceId));

        assertThat(first.getEmbedding()).isEqualTo(firstEmbedding);
        assertThat(second.getEmbedding()).isEqualTo(secondEmbedding);
        verify(chunkRepository, times(2)).save(first);
        verify(chunkRepository, times(2)).save(second);
    }

    @Test
    @DisplayName("execute - creates graph context before saving extracted triples")
    void execute_createsGraphContextForLoadedChunks() {
        Chunk chunk = chunk("chunk content", 0);
        Triple triple = new Triple("backpropagation", "USES", "chain rule");

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllBySourceId(sourceId)).thenReturn(List.of(chunk));
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});
        when(tripleExtractor.extract("chunk content")).thenReturn(List.of(triple));

        useCase.execute(GenerateSourceContextCommand.of(sourceId));

        verify(knowledgeGraphStore).createContext(List.of(chunk));

        ArgumentCaptor<List<KnowledgeTriple>> triplesCaptor = ArgumentCaptor.captor();
        verify(knowledgeGraphStore).saveAllForChunk(eq(chunk.getId()), triplesCaptor.capture());

        assertThat(triplesCaptor.getValue())
                .hasSize(1)
                .allSatisfy(knowledgeTriple ->
                        assertThat(knowledgeTriple.passage().chunkId()).isEqualTo(chunk.getId()));
    }

    @Test
    @DisplayName("execute - saves extracted triples with embeddings in the triple repository")
    void execute_savesExtractedTriplesWithEmbeddings() {
        Chunk chunk = chunk("chunk content", 0);
        Triple triple = new Triple("spring", "USES", "jpa");
        float[] tripleEmbedding = new float[]{0.7f, 0.8f};

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllBySourceId(sourceId)).thenReturn(List.of(chunk));
        when(embeddingProvider.embed("chunk content")).thenReturn(new float[]{0.1f});
        when(embeddingProvider.embed("spring-USES-jpa")).thenReturn(tripleEmbedding);
        when(tripleExtractor.extract("chunk content")).thenReturn(List.of(triple));

        useCase.execute(GenerateSourceContextCommand.of(sourceId));

        ArgumentCaptor<List<TripleExtracted>> triplesCaptor = ArgumentCaptor.captor();
        verify(tripleRepository).saveAll(triplesCaptor.capture());

        assertThat(triplesCaptor.getValue()).hasSize(1);
        TripleExtracted extracted = triplesCaptor.getValue().getFirst();
        assertThat(extracted.getKey()).isEqualTo("spring-USES-jpa");
        assertThat(extracted.getSuject()).isEqualTo("spring");
        assertThat(extracted.getPredicate()).isEqualTo("USES");
        assertThat(extracted.getObject()).isEqualTo("jpa");
        assertThat(extracted.getChunk()).isEqualTo(chunk);
        assertThat(extracted.getEmbedding()).isEqualTo(tripleEmbedding);
    }

    @Test
    @DisplayName("execute - marks each processed chunk and saves it after graph extraction")
    void execute_marksChunksAsProcessedAfterTripleExtraction() {
        Chunk chunk = chunk("chunk content", 0);

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllBySourceId(sourceId)).thenReturn(List.of(chunk));
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});
        when(tripleExtractor.extract(anyString())).thenReturn(List.of());

        useCase.execute(GenerateSourceContextCommand.of(sourceId));

        assertThat(chunk.isProcessed()).isTrue();
        verify(chunkRepository, times(2)).save(chunk);
    }

    @Test
    @DisplayName("execute - stores an empty triple list when extraction finds no triples")
    void execute_noTriplesExtracted_savesEmptyKnowledgeTriples() {
        Chunk chunk = chunk("chunk content", 0);

        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllBySourceId(sourceId)).thenReturn(List.of(chunk));
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});
        when(tripleExtractor.extract(anyString())).thenReturn(List.of());

        useCase.execute(GenerateSourceContextCommand.of(sourceId));

        ArgumentCaptor<List<KnowledgeTriple>> captor = ArgumentCaptor.captor();
        verify(knowledgeGraphStore).saveAllForChunk(eq(chunk.getId()), captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("execute - source not found throws RuntimeException with sourceId in message")
    void execute_sourceNotFound_throwsException() {
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(GenerateSourceContextCommand.of(sourceId)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(sourceId.toString());

        verifyNoInteractions(embeddingProvider, tripleExtractor, knowledgeGraphStore);
        verify(chunkRepository, never()).findAllBySourceId(any());
    }

    @Test
    @DisplayName("execute - no persisted chunks skips embedding and triple extraction")
    void execute_noChunks_skipsPerChunkWork() {
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllBySourceId(sourceId)).thenReturn(List.of());

        useCase.execute(GenerateSourceContextCommand.of(sourceId));

        verifyNoInteractions(embeddingProvider, tripleExtractor);
        verify(chunkRepository, never()).save(any(Chunk.class));
        verify(knowledgeGraphStore).createContext(List.of());
        verify(knowledgeGraphStore, never()).saveAllForChunk(any(), anyList());
    }
}
