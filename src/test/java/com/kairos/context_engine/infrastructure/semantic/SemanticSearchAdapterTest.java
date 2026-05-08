package com.kairos.context_engine.infrastructure.semantic;

import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.knowledge.Concept;
import com.kairos.context_engine.domain.model.retrieval.candidate.ConceptCandidate;
import com.kairos.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.context_engine.domain.model.retrieval.ranking.ScoredPassage;
import com.kairos.context_engine.domain.model.retrieval.source.RetrievalSource;
import com.kairos.context_engine.infrastructure.relational.entity.ChunkEntity;
import com.kairos.context_engine.infrastructure.relational.entity.SourceEntity;
import com.kairos.context_engine.infrastructure.relational.repository.chunk.JpaChunkRepository;
import com.kairos.context_engine.infrastructure.relational.projection.ConceptCandidateProjection;
import com.kairos.context_engine.infrastructure.relational.projection.PassageCandidateProjection;
import com.kairos.context_engine.infrastructure.relational.repository.source.JpaSourceRepository;
import com.kairos.context_engine.infrastructure.relational.repository.triple.JpaTripleRepository;
import com.kairos.context_engine.infrastructure.relational.semantic.SemanticSearchAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyList;

@ExtendWith(MockitoExtension.class)
@DisplayName("SemanticSearchAdapter")
class SemanticSearchAdapterTest {

    @Mock
    private JpaSourceRepository jpaSourceRepository;

    @Mock
    private JpaChunkRepository jpaChunkRepository;

    @Mock
    private JpaTripleRepository jpaTripleRepository;

    @InjectMocks
    private SemanticSearchAdapter adapter;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static final float[] QUERY_VECTOR = {0.1f, 0.2f, 0.3f, 0.4f};

    private SourceEntity sourceEntityA;
    private SourceEntity sourceEntityB;

    @BeforeEach
    void setUp() {
        sourceEntityA = new SourceEntity(
                UUID.randomUUID(), "Philosophy of Mind", "Content A"
        );
        sourceEntityB = new SourceEntity(
                UUID.randomUUID(), "Cognitive Science", "Content B"
        );
    }

    private ChunkEntity chunkEntity(UUID id, SourceEntity source, String content, int index) {
        return new ChunkEntity(id, source, content, index, false, QUERY_VECTOR);
    }

    private PassageCandidateProjection candidateProjection(UUID chunkId, double denseScore) {
        return new PassageCandidateProjection() {
            @Override
            public UUID getChunkId() {
                return chunkId;
            }

            @Override
            public double getDenseScore() {
                return denseScore;
            }
        };
    }

    private ConceptCandidateProjection conceptCandidateProjection(String name, double similarityScore) {
        return new ConceptCandidateProjection() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public double getSimilarityScore() {
                return similarityScore;
            }
        };
    }

    // =========================================================================
    // search()
    // =========================================================================



    // =========================================================================
    // findPassageCandidate()
    // =========================================================================

    @Nested
    @DisplayName("findPassageCandidate(float[], int)")
    class FindTopKMethod {

        @Test
        @DisplayName("forwards the query vector and k to jpaChunkRepository unchanged")
        void forwardsVectorAndKToRepository() {
            when(jpaChunkRepository.findCandidates(QUERY_VECTOR, 10)).thenReturn(List.of());

            adapter.findPassageCandidate(QUERY_VECTOR, 10);

            ArgumentCaptor<float[]> vectorCaptor = ArgumentCaptor.forClass(float[].class);
            verify(jpaChunkRepository).findCandidates(vectorCaptor.capture(), eq(10));
            assertThat(vectorCaptor.getValue()).isEqualTo(QUERY_VECTOR);
        }

        @Test
        @DisplayName("maps each candidate projection to a PassageCandidate")
        void mapsCandidateProjectionsToDomainModels() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();

            when(jpaChunkRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(
                            candidateProjection(idA, 0.91),
                            candidateProjection(idB, 0.72)
                    ));

            List<PassageCandidate> result = adapter.findPassageCandidate(QUERY_VECTOR, 2);

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(PassageCandidate::chunkId)
                    .containsExactly(idA, idB);
            assertThat(result)
                    .extracting(PassageCandidate::denseScore)
                    .containsExactly(0.91, 0.72);
        }


        @Test
        @DisplayName("maps chunk id and dense score from candidate projection")
        void mapsChunkIdAndDenseScore() {
            UUID id = UUID.randomUUID();

            when(jpaChunkRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(candidateProjection(id, 0.84)));

            List<PassageCandidate> result = adapter.findPassageCandidate(QUERY_VECTOR, 1);

            assertThat(result.getFirst().chunkId()).isEqualTo(id);
            assertThat(result.getFirst().denseScore()).isEqualTo(0.84);
        }

        @Test
        @DisplayName("preserves the cosine-distance ranking order returned by the repository")
        void preservesRankingOrder() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();
            UUID idC = UUID.randomUUID();

            when(jpaChunkRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(
                            candidateProjection(idA, 0.95),
                            candidateProjection(idB, 0.82),
                            candidateProjection(idC, 0.61)
                    ));

            List<PassageCandidate> result = adapter.findPassageCandidate(QUERY_VECTOR, 3);

            assertThat(result)
                    .extracting(PassageCandidate::chunkId)
                    .containsExactly(idA, idB, idC);
        }

        @Test
        @DisplayName("returns an empty list when the repository finds no matching chunks")
        void returnsEmptyListWhenNoResults() {
            when(jpaChunkRepository.findCandidates(any(), anyInt())).thenReturn(List.of());

            List<PassageCandidate> result = adapter.findPassageCandidate(QUERY_VECTOR, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("never interacts with jpaSourceRepository during findPassageCandidate")
        void doesNotTouchSourceRepositoryDuringFindTopK() {
            when(jpaChunkRepository.findCandidates(any(), anyInt())).thenReturn(List.of());

            adapter.findPassageCandidate(QUERY_VECTOR, 10);

            verifyNoInteractions(jpaSourceRepository);
        }

        @Test
        @DisplayName("propagates RuntimeException from jpaChunkRepository without wrapping")
        void propagatesRepositoryException() {
            when(jpaChunkRepository.findCandidates(any(), anyInt()))
                    .thenThrow(new RuntimeException("Connection pool exhausted"));

            assertThatThrownBy(() -> adapter.findPassageCandidate(QUERY_VECTOR, 10))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Connection pool exhausted");
        }
    }

    // =========================================================================
    // findConceptCandidate()
    // =========================================================================

    @Nested
    @DisplayName("findConceptCandidate(float[], int)")
    class FindConceptCandidateMethod {

        @Test
        @DisplayName("forwards the query vector and limit to jpaTripleRepository unchanged")
        void forwardsVectorAndLimitToRepository() {
            when(jpaTripleRepository.findCandidates(QUERY_VECTOR, 10)).thenReturn(List.of());

            adapter.findConceptCandidate(QUERY_VECTOR, 10);

            ArgumentCaptor<float[]> vectorCaptor = ArgumentCaptor.forClass(float[].class);
            verify(jpaTripleRepository).findCandidates(vectorCaptor.capture(), eq(10));
            assertThat(vectorCaptor.getValue()).isEqualTo(QUERY_VECTOR);
        }

        @Test
        @DisplayName("maps each candidate projection to a ConceptCandidate")
        void mapsCandidateProjectionsToDomainModels() {
            when(jpaTripleRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(
                            conceptCandidateProjection("consciousness", 0.91),
                            conceptCandidateProjection("qualia", 0.72)
                    ));

            List<ConceptCandidate> result = adapter.findConceptCandidate(QUERY_VECTOR, 2);

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(candidate -> candidate.concept().name())
                    .containsExactly("consciousness", "qualia");
            assertThat(result)
                    .extracting(ConceptCandidate::similarityScore)
                    .containsExactly(0.91, 0.72);
        }

        @Test
        @DisplayName("maps concept name and similarity score from candidate projection")
        void mapsNameAndSimilarityScore() {
            when(jpaTripleRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(conceptCandidateProjection("neural correlates", 0.84)));

            List<ConceptCandidate> result = adapter.findConceptCandidate(QUERY_VECTOR, 1);

            assertThat(result.getFirst().concept().name()).isEqualTo("neural correlates");
            assertThat(result.getFirst().similarityScore()).isEqualTo(0.84);
        }

        @Test
        @DisplayName("preserves the similarity ranking order returned by the repository")
        void preservesRankingOrder() {
            when(jpaTripleRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(
                            conceptCandidateProjection("intentionality", 0.95),
                            conceptCandidateProjection("phenomenology", 0.82),
                            conceptCandidateProjection("epistemology", 0.61)
                    ));

            List<ConceptCandidate> result = adapter.findConceptCandidate(QUERY_VECTOR, 3);

            assertThat(result)
                    .extracting(candidate -> candidate.concept().name())
                    .containsExactly("intentionality", "phenomenology", "epistemology");
        }

        @Test
        @DisplayName("returns an empty list when the repository finds no matching concepts")
        void returnsEmptyListWhenNoResults() {
            when(jpaTripleRepository.findCandidates(any(), anyInt())).thenReturn(List.of());

            List<ConceptCandidate> result = adapter.findConceptCandidate(QUERY_VECTOR, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("never interacts with jpaChunkRepository during findConceptCandidate")
        void doesNotTouchChunkRepository() {
            when(jpaTripleRepository.findCandidates(any(), anyInt())).thenReturn(List.of());

            adapter.findConceptCandidate(QUERY_VECTOR, 10);

            verifyNoInteractions(jpaChunkRepository);
        }

        @Test
        @DisplayName("never interacts with jpaSourceRepository during findConceptCandidate")
        void doesNotTouchSourceRepository() {
            when(jpaTripleRepository.findCandidates(any(), anyInt())).thenReturn(List.of());

            adapter.findConceptCandidate(QUERY_VECTOR, 10);

            verifyNoInteractions(jpaSourceRepository);
        }

        @Test
        @DisplayName("propagates RuntimeException from jpaTripleRepository without wrapping")
        void propagatesRepositoryException() {
            when(jpaTripleRepository.findCandidates(any(), anyInt()))
                    .thenThrow(new RuntimeException("Embedding computation failed"));

            assertThatThrownBy(() -> adapter.findConceptCandidate(QUERY_VECTOR, 10))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Embedding computation failed");
        }

        @Test
        @DisplayName("creates Concept instances with correct names from projection")
        void createsConceptInstancesCorrectly() {
            when(jpaTripleRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(conceptCandidateProjection("mind", 0.75)));

            List<ConceptCandidate> result = adapter.findConceptCandidate(QUERY_VECTOR, 1);

            Concept resultConcept = result.getFirst().concept();
            assertThat(resultConcept).isEqualTo(new Concept("mind"));
        }

        @Test
        @DisplayName("handles extreme similarity scores correctly (very low)")
        void handlesExtremeLowScores() {
            when(jpaTripleRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(conceptCandidateProjection("concept", 0.0)));

            List<ConceptCandidate> result = adapter.findConceptCandidate(QUERY_VECTOR, 1);

            assertThat(result.getFirst().similarityScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("handles extreme similarity scores correctly (very high)")
        void handlesExtremeHighScores() {
            when(jpaTripleRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(conceptCandidateProjection("concept", 1.0)));

            List<ConceptCandidate> result = adapter.findConceptCandidate(QUERY_VECTOR, 1);

            assertThat(result.getFirst().similarityScore()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("handles concept names with special characters")
        void handlesSpecialCharactersInNames() {
            String specialName = "concept-with_special.chars@2024";
            when(jpaTripleRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(conceptCandidateProjection(specialName, 0.85)));

            List<ConceptCandidate> result = adapter.findConceptCandidate(QUERY_VECTOR, 1);

            assertThat(result.getFirst().concept().name()).isEqualTo(specialName);
        }

        @Test
        @DisplayName("handles concept names with whitespace correctly")
        void handlesWhitespaceInNames() {
            String nameWithSpaces = "multi word concept phrase";
            when(jpaTripleRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(conceptCandidateProjection(nameWithSpaces, 0.75)));

            List<ConceptCandidate> result = adapter.findConceptCandidate(QUERY_VECTOR, 1);

            assertThat(result.getFirst().concept().name()).isEqualTo(nameWithSpaces);
        }

        @Test
        @DisplayName("processes large result sets efficiently")
        void handlesLargeResultSets() {
            List<ConceptCandidateProjection> largeResultSet = new java.util.ArrayList<>();
            for (int i = 0; i < 100; i++) {
                largeResultSet.add(conceptCandidateProjection("concept_" + i, 1.0 - (i * 0.01)));
            }

            when(jpaTripleRepository.findCandidates(any(), anyInt())).thenReturn(largeResultSet);

            List<ConceptCandidate> result = adapter.findConceptCandidate(QUERY_VECTOR, 100);

            assertThat(result).hasSize(100);
            assertThat(result.getFirst().concept().name()).isEqualTo("concept_0");
            assertThat(result.getLast().concept().name()).isEqualTo("concept_99");
        }

        @Test
        @DisplayName("calls repository only once per invocation")
        void callsRepositoryOnlyOnce() {
            when(jpaTripleRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(conceptCandidateProjection("concept", 0.85)));

            adapter.findConceptCandidate(QUERY_VECTOR, 5);

            verify(jpaTripleRepository, times(1)).findCandidates(any(float[].class), eq(5));
        }

        @Test
        @DisplayName("returns different results on successive calls with different parameters")
        void returnsDifferentResultsOnDifferentCalls() {
            ConceptCandidateProjection proj1 = conceptCandidateProjection("consciousness", 0.90);
            ConceptCandidateProjection proj2 = conceptCandidateProjection("phenomenology", 0.75);

            when(jpaTripleRepository.findCandidates(any(), eq(1)))
                    .thenReturn(List.of(proj1));
            when(jpaTripleRepository.findCandidates(any(), eq(2)))
                    .thenReturn(List.of(proj1, proj2));

            List<ConceptCandidate> result1 = adapter.findConceptCandidate(QUERY_VECTOR, 1);
            List<ConceptCandidate> result2 = adapter.findConceptCandidate(QUERY_VECTOR, 2);

            assertThat(result1).hasSize(1);
            assertThat(result2).hasSize(2);
        }

        @Test
        @DisplayName("maintains immutability - does not modify returned list")
        void maintainsImmutability() {
            when(jpaTripleRepository.findCandidates(any(), anyInt()))
                    .thenReturn(List.of(
                            conceptCandidateProjection("concept1", 0.80),
                            conceptCandidateProjection("concept2", 0.60)
                    ));

            List<ConceptCandidate> result = adapter.findConceptCandidate(QUERY_VECTOR, 2);
            int originalSize = result.size();

            // Try to modify (should fail or not affect returned list)
            assertThatThrownBy(() -> result.add(new ConceptCandidate(
                    new Concept("fake"), 0.5
            ))).isInstanceOf(UnsupportedOperationException.class);

            assertThat(result).hasSize(originalSize);
        }
    }

    // =========================================================================
    // findChunks()
    // =========================================================================

    @Nested
    @DisplayName("findChunks(List<UUID>)")
    class FindChunksMethod {

        @Test
        @DisplayName("returns empty list immediately when given a null ID list")
        void returnsEmptyForNullInput() {
            List<Chunk> result = adapter.findChunks(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(jpaChunkRepository);
        }

        @Test
        @DisplayName("returns empty list immediately when given an empty ID list")
        void returnsEmptyForEmptyInput() {
            List<Chunk> result = adapter.findChunks(List.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(jpaChunkRepository);
        }

        @Test
        @DisplayName("delegates to jpaChunkRepository.findAllById with the exact IDs provided")
        void delegatesWithExactIds() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();
            List<UUID> ids = List.of(idA, idB);

            when(jpaChunkRepository.findAllById(ids)).thenReturn(List.of());

            adapter.findChunks(ids);

            verify(jpaChunkRepository).findAllById(ids);
        }

        @Test
        @DisplayName("maps each retrieved ChunkEntity to a domain Chunk")
        void mapsFetchedEntitiesToDomainModels() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();

            ChunkEntity entityA = chunkEntity(idA, sourceEntityA, "First chunk",  0);
            ChunkEntity entityB = chunkEntity(idB, sourceEntityA, "Second chunk", 1);

            when(jpaChunkRepository.findAllById(anyList()))
                    .thenReturn(List.of(entityA, entityB));

            List<Chunk> result = adapter.findChunks(List.of(idA, idB));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Chunk::getContent)
                    .containsExactlyInAnyOrder("First chunk", "Second chunk");
        }

        @Test
        @DisplayName("returns partial results when the store does not contain all requested IDs")
        void returnsPartialResultsForMissingIds() {
            UUID presentId = UUID.randomUUID();
            UUID missingId = UUID.randomUUID();

            ChunkEntity presentEntity = chunkEntity(presentId, sourceEntityA, "Present", 0);

            // Store returns only the chunk that exists
            when(jpaChunkRepository.findAllById(anyList()))
                    .thenReturn(List.of(presentEntity));

            List<Chunk> result = adapter.findChunks(List.of(presentId, missingId));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getContent()).isEqualTo("Present");
        }

        @Test
        @DisplayName("does not guarantee ordering — result order matches what the repository returns")
        void doesNotGuaranteeOrdering() {
            // The Javadoc explicitly states order is not guaranteed.
            // This test documents and verifies that the adapter does NOT impose its own sort.
            UUID idFirst  = UUID.randomUUID();
            UUID idSecond = UUID.randomUUID();

            ChunkEntity entitySecond = chunkEntity(idSecond, sourceEntityA, "Second in DB", 1);
            ChunkEntity entityFirst  = chunkEntity(idFirst,  sourceEntityA, "First in DB",  0);

            // Repository deliberately returns them in reverse order
            when(jpaChunkRepository.findAllById(anyList()))
                    .thenReturn(List.of(entitySecond, entityFirst));

            List<Chunk> result = adapter.findChunks(List.of(idFirst, idSecond));

            assertThat(result)
                    .extracting(Chunk::getContent)
                    .containsExactly("Second in DB", "First in DB");
        }

        @Test
        @DisplayName("handles a single-element ID list correctly")
        void handlesSingleId() {
            UUID id = UUID.randomUUID();
            ChunkEntity entity = chunkEntity(id, sourceEntityA, "Single chunk", 0);

            when(jpaChunkRepository.findAllById(List.of(id))).thenReturn(List.of(entity));

            List<Chunk> result = adapter.findChunks(List.of(id));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getContent()).isEqualTo("Single chunk");
        }

        @Test
        @DisplayName("returns empty list when none of the requested IDs exist in the store")
        void returnsEmptyWhenNoneFound() {
            when(jpaChunkRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

            List<Chunk> result = adapter.findChunks(List.of(UUID.randomUUID(), UUID.randomUUID()));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("never interacts with jpaSourceRepository during findChunks")
        void doesNotTouchSourceRepository() {
            when(jpaChunkRepository.findAllById(anyList())).thenReturn(List.of());

            adapter.findChunks(List.of(UUID.randomUUID()));

            verifyNoInteractions(jpaSourceRepository);
        }

        @Test
        @DisplayName("propagates RuntimeException from jpaChunkRepository.findAllById without wrapping")
        void propagatesRepositoryException() {
            when(jpaChunkRepository.findAllById(anyList()))
                    .thenThrow(new RuntimeException("Deadlock detected"));

            assertThatThrownBy(() -> adapter.findChunks(List.of(UUID.randomUUID())))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Deadlock detected");
        }
    }

    // =========================================================================
    // Cross-method isolation
    // =========================================================================

    @Nested
    @DisplayName("hydrateAndRankChunks(List<ScoredPassage>)")
    class HydrateAndRankChunksMethod {

        @Test
        @DisplayName("returns empty list immediately when given a null scored passage list")
        void returnsEmptyForNullInput() {
            List<RankedChunk> result = adapter.hydrateAndRankChunks(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(jpaChunkRepository);
        }

        @Test
        @DisplayName("returns empty list immediately when given an empty scored passage list")
        void returnsEmptyForEmptyInput() {
            List<RankedChunk> result = adapter.hydrateAndRankChunks(List.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(jpaChunkRepository);
        }

        @Test
        @DisplayName("hydrates chunks while preserving scored passage order")
        void preservesScoredPassageOrder() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();
            ChunkEntity entityA = chunkEntity(idA, sourceEntityA, "First in repository", 0);
            ChunkEntity entityB = chunkEntity(idB, sourceEntityB, "Second in repository", 1);

            when(jpaChunkRepository.findAllById(anyList()))
                    .thenReturn(List.of(entityA, entityB));

            List<RankedChunk> result = adapter.hydrateAndRankChunks(List.of(
                    new ScoredPassage(idB, 0.91),
                    new ScoredPassage(idA, 0.72)
            ));

            assertThat(result)
                    .extracting(rankedChunk -> rankedChunk.chunk().getId())
                    .containsExactly(idB, idA);
            assertThat(result)
                    .extracting(RankedChunk::rank)
                    .containsExactly(1, 2);
            assertThat(result)
                    .extracting(RankedChunk::score)
                    .containsExactly(0.91, 0.72);
        }

        @Test
        @DisplayName("ignores scored passages whose chunk is missing from storage")
        void ignoresMissingChunks() {
            UUID presentId = UUID.randomUUID();
            UUID missingId = UUID.randomUUID();
            ChunkEntity present = chunkEntity(presentId, sourceEntityA, "Present chunk", 0);

            when(jpaChunkRepository.findAllById(anyList())).thenReturn(List.of(present));

            List<RankedChunk> result = adapter.hydrateAndRankChunks(List.of(
                    new ScoredPassage(missingId, 0.98),
                    new ScoredPassage(presentId, 0.75)
            ));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().chunk().getId()).isEqualTo(presentId);
            assertThat(result.getFirst().rank()).isEqualTo(1);
        }

        @Test
        @DisplayName("marks hydrated ranked chunks as graph-sourced")
        void usesGraphRetrievalSource() {
            UUID id = UUID.randomUUID();
            when(jpaChunkRepository.findAllById(anyList()))
                    .thenReturn(List.of(chunkEntity(id, sourceEntityA, "Graph result", 0)));

            List<RankedChunk> result = adapter.hydrateAndRankChunks(List.of(new ScoredPassage(id, 0.64)));

            assertThat(result)
                    .singleElement()
                    .satisfies(rankedChunk -> assertThat(rankedChunk.source()).isEqualTo(RetrievalSource.GRAPH));
        }
    }

    @Nested
    @DisplayName("cross-method isolation")
    class CrossMethodIsolation {


        @Test
        @DisplayName("findPassageCandidate() and findChunks() each call only jpaChunkRepository, never jpaSourceRepository")
        void chunkMethodsDoNotTouchSourceRepository() {
            when(jpaChunkRepository.findCandidates(any(), anyInt())).thenReturn(List.of());
            when(jpaChunkRepository.findAllById(anyList())).thenReturn(List.of());

            adapter.findPassageCandidate(QUERY_VECTOR, 5);
            adapter.findChunks(List.of(UUID.randomUUID()));

            verifyNoInteractions(jpaSourceRepository);
        }
    }
}
