package com.kairos.module.context_engine.use_case;

import com.kairos.module.context_engine.application.command.GenerateSourceContextCommand;
import com.kairos.module.context_engine.application.use_case.GenerateSourceContextUseCase;
import com.kairos.module.context_engine.domain.model.Triple;
import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.content.ChunkProcessingStatus;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.module.context_engine.domain.port.extraction.TripleExtractor;
import com.kairos.module.context_engine.domain.port.graph.KnowledgeGraphStore;
import com.kairos.module.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.module.context_engine.domain.port.repository.SourceRepository;
import com.kairos.module.context_engine.domain.port.repository.TripleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateSourceContextUseCaseTest {

    @Mock TripleExtractor tripleExtractor;
    @Mock EmbeddingProvider embeddingProvider;
    @Mock KnowledgeGraphStore knowledgeGraphStore;
    @Mock ChunkRepository chunkRepository;
    @Mock SourceRepository sourceRepository;
    @Mock TripleRepository tripleRepository;
    @InjectMocks GenerateSourceContextUseCase useCase;

    private UUID sourceId;
    private UUID authorId;
    private Source source;

    @BeforeEach
    void setUp() {
        sourceId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        source = new Source(sourceId, "Source", "content", authorId);
    }

    private Chunk chunk(String content, int index) {
        return Chunk.create(UUID.randomUUID(), source, content, index, false, null);
    }

    @Test
    void execute_processesOnlyPendingChunksAndMarksThemCompleted() {
        Chunk chunk = chunk("chunk", 0);
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
        when(embeddingProvider.embed("chunk")).thenReturn(new float[]{0.1f});
        when(tripleExtractor.extract("chunk")).thenReturn(List.of());

        useCase.execute(GenerateSourceContextCommand.of(sourceId));

        assertThat(chunk.getProcessingStatus()).isEqualTo(ChunkProcessingStatus.COMPLETED);
        verify(knowledgeGraphStore).savePassages(anyList(), eq(authorId));
        verify(knowledgeGraphStore).saveAllForChunk(eq(chunk.getId()), eq(authorId), anyList());
    }

    @Test
    void execute_marksFailedChunkAndContinuesWithNextChunk() {
        Chunk failed = chunk("failed", 0);
        Chunk completed = chunk("completed", 1);
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(failed, completed));
        when(embeddingProvider.embed("failed")).thenThrow(new RuntimeException("provider unavailable"));
        when(embeddingProvider.embed("completed")).thenReturn(new float[]{0.2f});
        when(tripleExtractor.extract("completed")).thenReturn(List.of());

        useCase.execute(GenerateSourceContextCommand.of(sourceId));

        assertThat(failed.getProcessingStatus()).isEqualTo(ChunkProcessingStatus.FAILED);
        assertThat(completed.getProcessingStatus()).isEqualTo(ChunkProcessingStatus.COMPLETED);
        verify(tripleExtractor, never()).extract("failed");
        verify(tripleExtractor).extract("completed");
    }

    @Test
    void executeClaimed_ignoresChunksOutsideSourceAndChunksNotProcessing() {
        Chunk claimed = new Chunk(UUID.randomUUID(), source, "claimed", 0,
                ChunkProcessingStatus.PROCESSING, null);
        Chunk pending = chunk("pending", 1);
        Source otherSource = new Source(UUID.randomUUID(), "Other", "content", authorId);
        Chunk other = new Chunk(UUID.randomUUID(), otherSource, "other", 0,
                ChunkProcessingStatus.PROCESSING, null);
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllByIds(anyList())).thenReturn(List.of(claimed, pending, other));
        when(embeddingProvider.embed("claimed")).thenReturn(new float[]{0.1f});
        when(tripleExtractor.extract("claimed")).thenReturn(List.of(new Triple("A", "R", "B")));
        when(embeddingProvider.embed(claimed.getId() + ":A-R-B")).thenReturn(new float[]{0.2f});

        useCase.executeClaimed(sourceId, List.of(claimed.getId(), pending.getId(), other.getId()));

        assertThat(claimed.getProcessingStatus()).isEqualTo(ChunkProcessingStatus.COMPLETED);
        assertThat(pending.getProcessingStatus()).isEqualTo(ChunkProcessingStatus.PENDING);
        verify(embeddingProvider, never()).embed("pending");
        verify(embeddingProvider, never()).embed("other");
    }

    @Test
    void execute_withNoPendingChunksDoesNoProcessingWork() {
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of());

        useCase.execute(GenerateSourceContextCommand.of(sourceId));

        verifyNoInteractions(embeddingProvider, tripleExtractor, knowledgeGraphStore, tripleRepository);
        verify(chunkRepository, never()).save(any());
    }
}
