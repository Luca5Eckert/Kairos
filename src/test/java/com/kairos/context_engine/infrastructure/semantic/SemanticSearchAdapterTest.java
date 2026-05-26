package com.kairos.context_engine.infrastructure.semantic;

import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.context_engine.domain.model.retrieval.candidate.TripleCandidate;
import com.kairos.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.context_engine.domain.model.retrieval.ranking.ScoredPassage;
import com.kairos.context_engine.domain.model.retrieval.source.RetrievalSource;
import com.kairos.context_engine.infrastructure.relational.entity.ChunkEntity;
import com.kairos.context_engine.infrastructure.relational.entity.SourceEntity;
import com.kairos.context_engine.infrastructure.relational.projection.PassageCandidateProjection;
import com.kairos.context_engine.infrastructure.relational.projection.TripleCandidateProjection;
import com.kairos.context_engine.infrastructure.relational.repository.chunk.JpaChunkRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SemanticSearchAdapter")
class SemanticSearchAdapterTest {

    private static final float[] QUERY_VECTOR = {0.1f, 0.2f, 0.3f, 0.4f};

    @Mock
    private JpaSourceRepository jpaSourceRepository;

    @Mock
    private JpaChunkRepository jpaChunkRepository;

    @Mock
    private JpaTripleRepository jpaTripleRepository;

    @InjectMocks
    private SemanticSearchAdapter adapter;

    private SourceEntity sourceEntityA;
    private SourceEntity sourceEntityB;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sourceEntityA = new SourceEntity(UUID.randomUUID(), "Philosophy of Mind", "Content A", userId);
        sourceEntityB = new SourceEntity(UUID.randomUUID(), "Cognitive Science", "Content B", userId);
    }

    @Nested
    @DisplayName("findPassageCandidate(float[], int)")
    class FindPassageCandidateMethod {

        @Test
        @DisplayName("forwards the query vector and k to jpaChunkRepository unchanged")
        void forwardsVectorAndKToRepository() {
            when(jpaChunkRepository.findCandidates(QUERY_VECTOR, userId, 10)).thenReturn(List.of());

            adapter.findPassageCandidate(QUERY_VECTOR, userId, 10);

            ArgumentCaptor<float[]> vectorCaptor = ArgumentCaptor.forClass(float[].class);
            verify(jpaChunkRepository).findCandidates(vectorCaptor.capture(), eq(userId), eq(10));
            assertThat(vectorCaptor.getValue()).isEqualTo(QUERY_VECTOR);
        }

        @Test
        @DisplayName("maps each candidate projection to a PassageCandidate")
        void mapsCandidateProjectionsToDomainModels() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();

            when(jpaChunkRepository.findCandidates(any(), any(UUID.class), anyInt()))
                    .thenReturn(List.of(
                            passageCandidateProjection(idA, 0.91),
                            passageCandidateProjection(idB, 0.72)
                    ));

            List<PassageCandidate> result = adapter.findPassageCandidate(QUERY_VECTOR, userId, 2);

            assertThat(result)
                    .extracting(PassageCandidate::chunkId)
                    .containsExactly(idA, idB);
            assertThat(result)
                    .extracting(PassageCandidate::denseScore)
                    .containsExactly(0.91, 0.72);
        }

        @Test
        @DisplayName("skips candidate projections with null dense scores")
        void skipsNullDenseScores() {
            when(jpaChunkRepository.findCandidates(any(), any(UUID.class), anyInt()))
                    .thenReturn(List.of(
                            passageCandidateProjection(UUID.randomUUID(), null),
                            passageCandidateProjection(UUID.randomUUID(), 0.73)
                    ));

            List<PassageCandidate> result = adapter.findPassageCandidate(QUERY_VECTOR, userId, 2);

            assertThat(result)
                    .singleElement()
                    .satisfies(candidate -> assertThat(candidate.denseScore()).isEqualTo(0.73));
        }

        @Test
        @DisplayName("propagates RuntimeException from jpaChunkRepository without wrapping")
        void propagatesRepositoryException() {
            when(jpaChunkRepository.findCandidates(any(), any(UUID.class), anyInt()))
                    .thenThrow(new RuntimeException("Connection pool exhausted"));

            assertThatThrownBy(() -> adapter.findPassageCandidate(QUERY_VECTOR, userId, 10))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Connection pool exhausted");
        }
    }

    @Nested
    @DisplayName("findTripleCandidates(float[], int)")
    class FindTripleCandidatesMethod {

        @Test
        @DisplayName("forwards the query vector and limit to jpaTripleRepository unchanged")
        void forwardsVectorAndLimitToRepository() {
            when(jpaTripleRepository.findTripleCandidates(QUERY_VECTOR, userId, 30)).thenReturn(List.of());

            adapter.findTripleCandidates(QUERY_VECTOR, userId, 30);

            ArgumentCaptor<float[]> vectorCaptor = ArgumentCaptor.forClass(float[].class);
            verify(jpaTripleRepository).findTripleCandidates(vectorCaptor.capture(), eq(userId), eq(30));
            assertThat(vectorCaptor.getValue()).isEqualTo(QUERY_VECTOR);
        }

        @Test
        @DisplayName("maps each triple projection to a TripleCandidate")
        void mapsTripleProjectionsToDomainModels() {
            UUID firstChunkId = UUID.randomUUID();
            UUID secondChunkId = UUID.randomUUID();
            when(jpaTripleRepository.findTripleCandidates(any(), any(UUID.class), anyInt()))
                    .thenReturn(List.of(
                            tripleCandidateProjection("mind-REL-consciousness", "mind", "REL", "consciousness", firstChunkId, 0.91),
                            tripleCandidateProjection("brain-USES-neurons", "brain", "USES", "neurons", secondChunkId, 0.72)
                    ));

            List<TripleCandidate> result = adapter.findTripleCandidates(QUERY_VECTOR, userId, 2);

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(TripleCandidate::key)
                    .containsExactly("mind-REL-consciousness", "brain-USES-neurons");
            assertThat(result)
                    .extracting(TripleCandidate::chunkId)
                    .containsExactly(firstChunkId, secondChunkId);
            assertThat(result)
                    .extracting(TripleCandidate::similarityScore)
                    .containsExactly(0.91, 0.72);
        }

        @Test
        @DisplayName("preserves the proximity ranking order returned by the repository")
        void preservesRankingOrder() {
            when(jpaTripleRepository.findTripleCandidates(any(), any(UUID.class), anyInt()))
                    .thenReturn(List.of(
                            tripleCandidateProjection("a-R-b", "a", "R", "b", UUID.randomUUID(), 0.95),
                            tripleCandidateProjection("c-R-d", "c", "R", "d", UUID.randomUUID(), 0.82),
                            tripleCandidateProjection("e-R-f", "e", "R", "f", UUID.randomUUID(), 0.61)
                    ));

            List<TripleCandidate> result = adapter.findTripleCandidates(QUERY_VECTOR, userId, 3);

            assertThat(result)
                    .extracting(TripleCandidate::key)
                    .containsExactly("a-R-b", "c-R-d", "e-R-f");
        }

        @Test
        @DisplayName("returns an empty list when no triple candidates are found")
        void returnsEmptyWhenNoResults() {
            when(jpaTripleRepository.findTripleCandidates(any(), any(UUID.class), anyInt())).thenReturn(List.of());

            List<TripleCandidate> result = adapter.findTripleCandidates(QUERY_VECTOR, userId, 30);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("skips projections with incomplete triple data")
        void skipsIncompleteTripleData() {
            when(jpaTripleRepository.findTripleCandidates(any(), any(UUID.class), anyInt()))
                    .thenReturn(List.of(
                            tripleCandidateProjection(null, "mind", "REL", "consciousness", UUID.randomUUID(), 0.91),
                            tripleCandidateProjection("blank-subject", " ", "REL", "consciousness", UUID.randomUUID(), 0.82),
                            tripleCandidateProjection("missing-score", "mind", "REL", "consciousness", UUID.randomUUID(), null),
                            tripleCandidateProjection("valid", "mind", "REL", "consciousness", UUID.randomUUID(), 0.7)
                    ));

            List<TripleCandidate> result = adapter.findTripleCandidates(QUERY_VECTOR, userId, 4);

            assertThat(result)
                    .singleElement()
                    .satisfies(candidate -> assertThat(candidate.key()).isEqualTo("valid"));
        }

        @Test
        @DisplayName("skips projections with non-finite similarity")
        void skipsNonFiniteSimilarity() {
            when(jpaTripleRepository.findTripleCandidates(any(), any(UUID.class), anyInt()))
                    .thenReturn(List.of(
                            tripleCandidateProjection("nan", "mind", "REL", "consciousness", UUID.randomUUID(), Double.NaN),
                            tripleCandidateProjection("positive-infinity", "brain", "REL", "neurons", UUID.randomUUID(), Double.POSITIVE_INFINITY),
                            tripleCandidateProjection("valid", "spring", "IMPLEMENTS", "repositories", UUID.randomUUID(), 0.7)
                    ));

            List<TripleCandidate> result = adapter.findTripleCandidates(QUERY_VECTOR, userId, 3);

            assertThat(result)
                    .singleElement()
                    .satisfies(candidate -> assertThat(candidate.key()).isEqualTo("valid"));
        }

        @Test
        @DisplayName("never interacts with chunk or source repositories during triple lookup")
        void doesNotTouchOtherRepositories() {
            when(jpaTripleRepository.findTripleCandidates(any(), any(UUID.class), anyInt())).thenReturn(List.of());

            adapter.findTripleCandidates(QUERY_VECTOR, userId, 30);

            verifyNoInteractions(jpaChunkRepository, jpaSourceRepository);
        }

        @Test
        @DisplayName("propagates RuntimeException from jpaTripleRepository without wrapping")
        void propagatesRepositoryException() {
            when(jpaTripleRepository.findTripleCandidates(any(), any(UUID.class), anyInt()))
                    .thenThrow(new RuntimeException("Vector index unavailable"));

            assertThatThrownBy(() -> adapter.findTripleCandidates(QUERY_VECTOR, userId, 30))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Vector index unavailable");
        }
    }

    @Nested
    @DisplayName("findChunks(List<UUID>)")
    class FindChunksMethod {

        @Test
        @DisplayName("returns empty list immediately when given a null ID list")
        void returnsEmptyForNullInput() {
            List<Chunk> result = adapter.findChunks(null, userId);

            assertThat(result).isEmpty();
            verifyNoInteractions(jpaChunkRepository);
        }

        @Test
        @DisplayName("maps each retrieved ChunkEntity to a domain Chunk")
        void mapsFetchedEntitiesToDomainModels() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();

            when(jpaChunkRepository.findAllByIdInAndSource_AuthorId(anyList(), eq(userId)))
                    .thenReturn(List.of(
                            chunkEntity(idA, sourceEntityA, "First chunk", 0),
                            chunkEntity(idB, sourceEntityA, "Second chunk", 1)
                    ));

            List<Chunk> result = adapter.findChunks(List.of(idA, idB), userId);

            assertThat(result)
                    .extracting(Chunk::getContent)
                    .containsExactlyInAnyOrder("First chunk", "Second chunk");
        }

        @Test
        @DisplayName("returns empty list when none of the requested IDs exist in the store")
        void returnsEmptyWhenNoneFound() {
            when(jpaChunkRepository.findAllByIdInAndSource_AuthorId(anyList(), eq(userId))).thenReturn(Collections.emptyList());

            List<Chunk> result = adapter.findChunks(List.of(UUID.randomUUID(), UUID.randomUUID()), userId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hydrateAndRankChunks(List<ScoredPassage>)")
    class HydrateAndRankChunksMethod {

        @Test
        @DisplayName("returns empty list immediately when given a null scored passage list")
        void returnsEmptyForNullInput() {
            List<RankedChunk> result = adapter.hydrateAndRankChunks(null, userId);

            assertThat(result).isEmpty();
            verifyNoInteractions(jpaChunkRepository);
        }

        @Test
        @DisplayName("hydrates chunks while preserving scored passage order")
        void preservesScoredPassageOrder() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();

            when(jpaChunkRepository.findAllByIdInAndSource_AuthorId(anyList(), eq(userId)))
                    .thenReturn(List.of(
                            chunkEntity(idA, sourceEntityA, "First in repository", 0),
                            chunkEntity(idB, sourceEntityB, "Second in repository", 1)
                    ));

            List<RankedChunk> result = adapter.hydrateAndRankChunks(List.of(
                    new ScoredPassage(idB, 0.91),
                    new ScoredPassage(idA, 0.72)
            ), userId);

            assertThat(result)
                    .extracting(rankedChunk -> rankedChunk.chunk().getId())
                    .containsExactly(idB, idA);
            assertThat(result)
                    .extracting(RankedChunk::rank)
                    .containsExactly(1, 2);
            assertThat(result)
                    .extracting(RankedChunk::score)
                    .containsExactly(0.91, 0.72);
            assertThat(result)
                    .extracting(RankedChunk::source)
                    .containsExactly(RetrievalSource.GRAPH, RetrievalSource.GRAPH);
        }

        @Test
        @DisplayName("deduplicates hydrated chunks by content and keeps contiguous ranks")
        void deduplicatesHydratedChunksByContent() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();
            UUID idC = UUID.randomUUID();

            when(jpaChunkRepository.findAllByIdInAndSource_AuthorId(anyList(), eq(userId)))
                    .thenReturn(List.of(
                            chunkEntity(idA, sourceEntityA, "Repeated content", 0),
                            chunkEntity(idB, sourceEntityB, "Repeated content", 0),
                            chunkEntity(idC, sourceEntityB, "Unique content", 1)
                    ));

            List<RankedChunk> result = adapter.hydrateAndRankChunks(List.of(
                    new ScoredPassage(idA, 0.91),
                    new ScoredPassage(idB, 0.82),
                    new ScoredPassage(idC, 0.72)
            ), userId);

            assertThat(result)
                    .extracting(rankedChunk -> rankedChunk.chunk().getId())
                    .containsExactly(idA, idC);
            assertThat(result)
                    .extracting(RankedChunk::rank)
                    .containsExactly(1, 2);
        }
    }

    private ChunkEntity chunkEntity(UUID id, SourceEntity source, String content, int index) {
        return new ChunkEntity(id, source, content, index, false, QUERY_VECTOR);
    }

    private PassageCandidateProjection passageCandidateProjection(UUID chunkId, Double denseScore) {
        return new PassageCandidateProjection() {
            @Override
            public UUID getChunkId() {
                return chunkId;
            }

            @Override
            public Double getDenseScore() {
                return denseScore;
            }
        };
    }

    private TripleCandidateProjection tripleCandidateProjection(
            String key,
            String subject,
            String predicate,
            String object,
            UUID chunkId,
            Double similarity
    ) {
        return new TripleCandidateProjection() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getSubject() {
                return subject;
            }

            @Override
            public String getPredicate() {
                return predicate;
            }

            @Override
            public String getObject() {
                return object;
            }

            @Override
            public UUID getChunkId() {
                return chunkId;
            }

            @Override
            public Double getSimilarity() {
                return similarity;
            }
        };
    }
}
