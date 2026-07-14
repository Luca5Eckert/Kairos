package com.kairos.context_engine.use_case;

import com.kairos.context_engine.application.command.GenerateSourceContextCommand;
import com.kairos.context_engine.application.use_case.GenerateSourceContextUseCase;
import com.kairos.context_engine.domain.model.knowledge.Passage;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GenerateSourceContextUseCase}.
 * Tests the orchestration of chunk embedding, triple extraction, and knowledge graph generation.
 *
 * Testing Strategy:
 * - Source resolution and error handling
 * - Chunk loading (unprocessed chunks only)
 * - Embedding generation for chunks
 * - Triple extraction and embedding
 * - Knowledge graph persistence
 * - Transaction semantics and ordering
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateSourceContextUseCase")
class GenerateSourceContextUseCaseTest {

    @Mock
    private TripleExtractor tripleExtractor;

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private KnowledgeGraphStore knowledgeGraphStore;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private TripleRepository tripleRepository;

    @Captor
    private ArgumentCaptor<List<TripleExtracted>> tripleExtractedCaptor;

    @Captor
    private ArgumentCaptor<List<KnowledgeTriple>> knowledgeTripleCaptor;

    @Captor
    private ArgumentCaptor<List<Passage>> passageCaptor;

    @InjectMocks
    private GenerateSourceContextUseCase useCase;

    private Source source;
    private UUID sourceId;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        sourceId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        source = new Source(sourceId, "Clean Code", "some content", authorId);
    }

    private Chunk chunk(String content, int index) {
        return Chunk.create(UUID.randomUUID(), source, content, index, false, null);
    }

    private Triple triple(String subject, String predicate, String object) {
        return new Triple(subject, predicate, object);
    }

    // =========================================================================
    // Execution Flow & Happy Path
    // =========================================================================

    @Nested
    @DisplayName("execute(GenerateSourceContextCommand)")
    class ExecuteMethod {

        @Test
        @DisplayName("resolves source from repository by ID")
        void resolveSource() {
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            verify(sourceRepository).findById(sourceId);
        }

        @Test
        @DisplayName("loads unprocessed chunks for the source")
        void loadUnprocessedChunks() {
            Chunk chunk = chunk("content", 0);
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content")).thenReturn(new float[]{0.1f});
            when(tripleExtractor.extract("content")).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            verify(chunkRepository).findAllNotProcessedBySourceId(sourceId);
        }

        @Test
        @DisplayName("throws RuntimeException when source not found")
        void throwsExceptionWhenSourceNotFound() {
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(GenerateSourceContextCommand.of(sourceId)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(sourceId.toString())
                    .hasMessageContaining("Source not found");

            // Verify no downstream operations when source fails
            verifyNoInteractions(chunkRepository, embeddingProvider, tripleExtractor);
        }

        @Test
        @DisplayName("skips processing when no unprocessed chunks exist")
        void skipsProcessingWhenNoChunks() {
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            verifyNoInteractions(embeddingProvider, tripleExtractor, tripleRepository);
            verify(chunkRepository, never()).save(any(Chunk.class));
            // knowledgeGraphStore.savePassages() is still called with empty list
            verify(knowledgeGraphStore).savePassages(List.of(), authorId);
            verify(knowledgeGraphStore, never()).saveAllForChunk(any(UUID.class), any(UUID.class), anyList());
        }

        @Test
        @DisplayName("throws IllegalStateException when source has no author")
        void throwsExceptionWhenSourceHasNoAuthor() {
            Source authorlessSource = new Source(sourceId, "Legacy", "content");
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(authorlessSource));

            assertThatThrownBy(() -> useCase.execute(GenerateSourceContextCommand.of(sourceId)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Source author is required");

            verifyNoInteractions(chunkRepository, embeddingProvider, knowledgeGraphStore, tripleExtractor, tripleRepository);
        }
    }

    // =========================================================================
    // Chunk Embedding Phase
    // =========================================================================

    @Nested
    @DisplayName("Chunk Embedding Phase")
    class ChunkEmbeddingPhase {

        @Test
        @DisplayName("embeds each chunk's content")
        void embedsChunkContent() {
            Chunk first = chunk("first content", 0);
            Chunk second = chunk("second content", 1);
            float[] firstEmbedding = new float[]{0.1f, 0.2f};
            float[] secondEmbedding = new float[]{0.3f, 0.4f};

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId))
                    .thenReturn(List.of(first, second));
            when(embeddingProvider.embed("first content")).thenReturn(firstEmbedding);
            when(embeddingProvider.embed("second content")).thenReturn(secondEmbedding);
            when(tripleExtractor.extract(anyString())).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            assertThat(first.getEmbedding()).isEqualTo(firstEmbedding);
            assertThat(second.getEmbedding()).isEqualTo(secondEmbedding);
        }

        @Test
        @DisplayName("saves each chunk after embedding")
        void savesChunksAfterEmbedding() {
            Chunk chunk = chunk("content", 0);
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content")).thenReturn(new float[]{0.1f});
            when(tripleExtractor.extract("content")).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            // Save called twice: once in embedChunks, once in createContextForKnowledgeGraph
            verify(chunkRepository, times(2)).save(chunk);
        }

        @Test
        @DisplayName("calls embedding provider once per chunk")
        void callsEmbeddingProviderOncePerChunk() {
            Chunk first = chunk("first", 0);
            Chunk second = chunk("second", 1);

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId))
                    .thenReturn(List.of(first, second));
            when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});
            when(tripleExtractor.extract(anyString())).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            // Called twice: once for "first" chunk, once for "second" chunk (in embedChunks phase)
            verify(embeddingProvider, times(2)).embed(anyString());
        }

        @Test
        @DisplayName("stops before graph generation when a later chunk embedding fails")
        void stopsBeforeGraphGenerationWhenChunkEmbeddingFails() {
            Chunk first = chunk("first", 0);
            Chunk second = chunk("second", 1);

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId))
                    .thenReturn(List.of(first, second));
            when(embeddingProvider.embed("first")).thenReturn(new float[]{0.1f});
            when(embeddingProvider.embed("second"))
                    .thenThrow(new RuntimeException("Second embedding failed"));

            assertThatThrownBy(() -> useCase.execute(GenerateSourceContextCommand.of(sourceId)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Second embedding failed");

            assertThat(first.getEmbedding()).containsExactly(0.1f);
            assertThat(first.isProcessed()).isFalse();
            assertThat(second.getEmbedding()).isNull();
            assertThat(second.isProcessed()).isFalse();
            verify(chunkRepository).save(first);
            verify(chunkRepository, never()).save(second);
            verifyNoInteractions(knowledgeGraphStore, tripleExtractor, tripleRepository);
        }
    }

    // =========================================================================
    // Knowledge Graph Generation Phase
    // =========================================================================

    @Nested
    @DisplayName("Knowledge Graph Generation Phase")
    class KnowledgeGraphGenerationPhase {

        @Test
        @DisplayName("saves passages for all chunks")
        void savesPassages() {
            Chunk chunk = chunk("content", 0);
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content")).thenReturn(new float[]{0.1f});
            when(tripleExtractor.extract("content")).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            verify(knowledgeGraphStore).savePassages(passageCaptor.capture(), eq(authorId));
            List<Passage> savedPassages = passageCaptor.getValue();
            assertThat(savedPassages).hasSize(1);
            assertThat(savedPassages.getFirst().chunkId()).isEqualTo(chunk.getId());
        }

        @Test
        @DisplayName("extracts triples from chunk content")
        void extractsTriplesFromContent() {
            Chunk chunk = chunk("machine learning content", 0);
            Triple triple = triple("neural network", "USES", "backpropagation");

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});
            when(tripleExtractor.extract("machine learning content")).thenReturn(List.of(triple));

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            verify(tripleExtractor).extract("machine learning content");
        }

        @Test
        @DisplayName("generates embeddings for each extracted triple")
        void generatesTripleEmbeddings() {
            Chunk chunk = chunk("content", 0);
            Triple triple = triple("spring", "USES", "jpa");

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content")).thenReturn(new float[]{0.1f});
            String tripleKey = chunk.getId() + ":spring-USES-jpa";
            when(embeddingProvider.embed(tripleKey)).thenReturn(new float[]{0.7f, 0.8f});
            when(tripleExtractor.extract("content")).thenReturn(List.of(triple));

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            verify(embeddingProvider).embed(tripleKey);
        }

        @Test
        @DisplayName("saves extracted triples with embeddings to repository")
        void savesExtractedTriplesToRepository() {
            Chunk chunk = chunk("content", 0);
            Triple triple = triple("spring", "USES", "jpa");
            float[] tripleEmbedding = new float[]{0.7f, 0.8f};

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content")).thenReturn(new float[]{0.1f});
            String tripleKey = chunk.getId() + ":spring-USES-jpa";
            when(embeddingProvider.embed(tripleKey)).thenReturn(tripleEmbedding);
            when(tripleExtractor.extract("content")).thenReturn(List.of(triple));

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            verify(tripleRepository).saveAll(tripleExtractedCaptor.capture());
            List<TripleExtracted> saved = tripleExtractedCaptor.getValue();

            assertThat(saved).hasSize(1);
            TripleExtracted extracted = saved.getFirst();
            assertThat(extracted.getKey()).isEqualTo(tripleKey);
            assertThat(extracted.getSuject()).isEqualTo("spring");
            assertThat(extracted.getPredicate()).isEqualTo("USES");
            assertThat(extracted.getObject()).isEqualTo("jpa");
            assertThat(extracted.getEmbedding()).isEqualTo(tripleEmbedding);
        }

        @Test
        @DisplayName("saves knowledge triples to graph store by chunk")
        void savesKnowledgeTriplesToGraph() {
            Chunk chunk = chunk("content", 0);
            Triple triple = triple("concept", "RELATES_TO", "other");

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});
            when(tripleExtractor.extract("content")).thenReturn(List.of(triple));

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            verify(knowledgeGraphStore).saveAllForChunk(eq(chunk.getId()), eq(authorId), knowledgeTripleCaptor.capture());
            List<KnowledgeTriple> saved = knowledgeTripleCaptor.getValue();

            assertThat(saved).hasSize(1);
            assertThat(saved.getFirst().passage().chunkId()).isEqualTo(chunk.getId());
        }

        @Test
        @DisplayName("handles empty triple extraction gracefully")
        void handlesEmptyTripleExtraction() {
            Chunk chunk = chunk("content", 0);
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content")).thenReturn(new float[]{0.1f});
            when(tripleExtractor.extract("content")).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            verify(tripleRepository).saveAll(tripleExtractedCaptor.capture());
            assertThat(tripleExtractedCaptor.getValue()).isEmpty();

            verify(knowledgeGraphStore).saveAllForChunk(eq(chunk.getId()), eq(authorId), knowledgeTripleCaptor.capture());
            assertThat(knowledgeTripleCaptor.getValue()).isEmpty();
        }

        @Test
        @DisplayName("marks chunks as processed after extraction")
        void marksChunksAsProcessed() {
            Chunk chunk = chunk("content", 0);
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content")).thenReturn(new float[]{0.1f});
            when(tripleExtractor.extract("content")).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            assertThat(chunk.isProcessed()).isTrue();
        }

        @Test
        @DisplayName("marks chunk as processed only after triples and graph relationships are saved")
        void marksChunkAsProcessedOnlyAfterTripleAndGraphPersistence() {
            Chunk chunk = chunk("content", 0);
            Triple triple = triple("spring", "USES", "jpa");

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content")).thenReturn(new float[]{0.1f});
            when(embeddingProvider.embed(chunk.getId() + ":spring-USES-jpa")).thenReturn(new float[]{0.2f});
            when(tripleExtractor.extract("content")).thenReturn(List.of(triple));
            doAnswer(invocation -> {
                assertThat(chunk.isProcessed()).isFalse();
                return null;
            }).when(tripleRepository).saveAll(anyList());
            doAnswer(invocation -> {
                assertThat(chunk.isProcessed()).isFalse();
                return null;
            }).when(knowledgeGraphStore).saveAllForChunk(eq(chunk.getId()), eq(authorId), anyList());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            assertThat(chunk.isProcessed()).isTrue();
            var inOrder = inOrder(tripleRepository, knowledgeGraphStore, chunkRepository);
            inOrder.verify(tripleRepository).saveAll(anyList());
            inOrder.verify(knowledgeGraphStore).saveAllForChunk(eq(chunk.getId()), eq(authorId), anyList());
            inOrder.verify(chunkRepository).save(chunk);
        }
    }

    // =========================================================================
    // Integration & Ordering Tests
    // =========================================================================

    @Nested
    @DisplayName("Integration & Execution Order")
    class IntegrationTests {

        @Test
        @DisplayName("executes operations in correct order: embed -> graph generation -> mark processed")
        void executesInCorrectOrder() {
            Chunk chunk = chunk("content", 0);
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content")).thenReturn(new float[]{0.1f});
            when(tripleExtractor.extract("content")).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            var inOrder = inOrder(
                    chunkRepository,
                    embeddingProvider,
                    knowledgeGraphStore,
                    tripleExtractor,
                    tripleRepository
            );

            // 1. Embedding provider called for chunk content
            inOrder.verify(embeddingProvider).embed("content");
            // 2. First save after embedding
            inOrder.verify(chunkRepository).save(chunk);
            // 3. Graph operations (passages saved)
            inOrder.verify(knowledgeGraphStore).savePassages(anyList(), eq(authorId));
            // 4. Graph operations (triples saved)
            inOrder.verify(tripleRepository).saveAll(anyList());
            inOrder.verify(knowledgeGraphStore).saveAllForChunk(any(UUID.class), eq(authorId), anyList());
            // 5. Second save after marking processed
            inOrder.verify(chunkRepository).save(chunk);
        }

        @Test
        @DisplayName("processes multiple chunks independently")
        void processesMultipleChunksIndependently() {
            Chunk first = chunk("first content", 0);
            Chunk second = chunk("second content", 1);

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId))
                    .thenReturn(List.of(first, second));
            when(embeddingProvider.embed("first content")).thenReturn(new float[]{0.1f});
            when(embeddingProvider.embed("second content")).thenReturn(new float[]{0.2f});
            when(tripleExtractor.extract("first content")).thenReturn(List.of());
            when(tripleExtractor.extract("second content")).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            // Verify both chunks processed
            verify(knowledgeGraphStore, times(2)).saveAllForChunk(any(UUID.class), eq(authorId), anyList());
            verify(chunkRepository, times(4)).save(any(Chunk.class)); // 2x per chunk
        }

        @Test
        @DisplayName("saves passages once for all chunks")
        void savesPassagesOnceForAllChunks() {
            Chunk first = chunk("first", 0);
            Chunk second = chunk("second", 1);

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId))
                    .thenReturn(List.of(first, second));
            when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});
            when(tripleExtractor.extract(anyString())).thenReturn(List.of());

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            verify(knowledgeGraphStore, times(1)).savePassages(passageCaptor.capture(), eq(authorId));
            assertThat(passageCaptor.getValue()).hasSize(2);
        }
    }

    // =========================================================================
    // Error Handling & Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("Error Handling & Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("propagates embedding provider exceptions")
        void propagatesEmbeddingException() {
            Chunk chunk = chunk("content", 0);
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content"))
                    .thenThrow(new RuntimeException("Embedding service unavailable"));

            assertThatThrownBy(() -> useCase.execute(GenerateSourceContextCommand.of(sourceId)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Embedding service unavailable");
        }

        @Test
        @DisplayName("propagates triple embedding exceptions without saving triples or marking chunks")
        void propagatesTripleEmbeddingExceptionWithoutPersistingTriplesOrMarkingChunks() {
            Chunk first = chunk("first", 0);
            Chunk second = chunk("second", 1);
            Triple triple = triple("A", "RELATES_TO", "B");

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId))
                    .thenReturn(List.of(first, second));
            when(embeddingProvider.embed("first")).thenReturn(new float[]{0.1f});
            when(embeddingProvider.embed("second")).thenReturn(new float[]{0.2f});
            when(tripleExtractor.extract("first")).thenReturn(List.of(triple));
            when(embeddingProvider.embed(first.getId() + ":A-RELATES_TO-B"))
                    .thenThrow(new RuntimeException("Triple embedding failed"));

            assertThatThrownBy(() -> useCase.execute(GenerateSourceContextCommand.of(sourceId)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Triple embedding failed");

            assertThat(first.isProcessed()).isFalse();
            assertThat(second.isProcessed()).isFalse();
            verify(knowledgeGraphStore).savePassages(passageCaptor.capture(), eq(authorId));
            assertThat(passageCaptor.getValue())
                    .extracting(Passage::chunkId)
                    .containsExactly(first.getId(), second.getId());
            verify(tripleRepository, never()).saveAll(anyList());
            verify(knowledgeGraphStore, never()).saveAllForChunk(any(UUID.class), any(UUID.class), anyList());
            verify(tripleExtractor, never()).extract("second");
            verify(chunkRepository, times(1)).save(first);
            verify(chunkRepository, times(1)).save(second);
        }

        @Test
        @DisplayName("propagates triple extraction exceptions")
        void propagatesExtractionException() {
            Chunk chunk = chunk("content", 0);
            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content")).thenReturn(new float[]{0.1f});
            when(tripleExtractor.extract("content"))
                    .thenThrow(new RuntimeException("Triple extraction failed"));

            assertThatThrownBy(() -> useCase.execute(GenerateSourceContextCommand.of(sourceId)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Triple extraction failed");
        }

        @Test
        @DisplayName("propagates repository exceptions")
        void propagatesRepositoryException() {
            when(sourceRepository.findById(sourceId))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> useCase.execute(GenerateSourceContextCommand.of(sourceId)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection failed");
        }

        @Test
        @DisplayName("handles multiple triples per chunk")
        void handlesMultipleTriplesPerChunk() {
            Chunk chunk = chunk("content", 0);
            List<Triple> triples = List.of(
                    triple("A", "R1", "B"),
                    triple("B", "R2", "C"),
                    triple("C", "R3", "D")
            );

            when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(chunkRepository.findAllNotProcessedBySourceId(sourceId)).thenReturn(List.of(chunk));
            when(embeddingProvider.embed("content")).thenReturn(new float[]{0.1f});
            when(embeddingProvider.embed(chunk.getId() + ":A-R1-B")).thenReturn(new float[]{0.5f});
            when(embeddingProvider.embed(chunk.getId() + ":B-R2-C")).thenReturn(new float[]{0.6f});
            when(embeddingProvider.embed(chunk.getId() + ":C-R3-D")).thenReturn(new float[]{0.7f});
            when(tripleExtractor.extract("content")).thenReturn(triples);

            useCase.execute(GenerateSourceContextCommand.of(sourceId));

            verify(tripleRepository).saveAll(tripleExtractedCaptor.capture());
            assertThat(tripleExtractedCaptor.getValue()).hasSize(3);

            verify(knowledgeGraphStore).saveAllForChunk(eq(chunk.getId()), eq(authorId), knowledgeTripleCaptor.capture());
            assertThat(knowledgeTripleCaptor.getValue()).hasSize(3);
        }
    }
}
