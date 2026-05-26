package com.kairos.context_engine.use_case;

import com.kairos.context_engine.application.query.SearchSourceQuery;
import com.kairos.context_engine.application.use_case.SearchSourceUseCase;
import com.kairos.context_engine.domain.model.SearchResult;
import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.content.Source;
import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.context_engine.domain.model.knowledge.Passage;
import com.kairos.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.context_engine.domain.model.retrieval.candidate.TripleCandidate;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchResult;
import com.kairos.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.context_engine.domain.model.retrieval.ranking.ScoredPassage;
import com.kairos.context_engine.domain.model.retrieval.seed.ConceptSeedTarget;
import com.kairos.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.context_engine.domain.model.retrieval.seed.PassageSeedTarget;
import com.kairos.context_engine.domain.model.retrieval.source.RetrievalSource;
import com.kairos.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.context_engine.domain.port.graph.KnowledgeGraphSearch;
import com.kairos.context_engine.domain.port.recognition.RecognitionMemory;
import com.kairos.context_engine.domain.port.semantic.SemanticSearch;
import com.kairos.share.security.context.RequestContext;
import com.kairos.share.security.context.RequestContextProvider;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchSourceUseCase")
class SearchSourceUseCaseTest {

    private static final float[] QUERY_VECTOR = new float[]{0.1f, 0.2f, 0.3f};
    private static final String SEARCH_TERM = "What is the philosophy of mind?";

    @Mock
    private EmbeddingProvider embeddingPort;

    @Mock
    private KnowledgeGraphSearch knowledgeGraphSearch;

    @Mock
    private SemanticSearch semanticSearch;

    @Mock
    private RecognitionMemory recognitionMemory;

    @Mock
    private RequestContextProvider requestContextProvider;

    @InjectMocks
    private SearchSourceUseCase useCase;

    private Source source;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        source = new Source(UUID.randomUUID(), "Philosophy of Mind", "Source content", userId);
        when(requestContextProvider.getRequestContext())
                .thenReturn(new RequestContext(userId, "lucas@example.com", List.of()));
    }

    @Test
    @DisplayName("builds a graph request only from strong passage candidates")
    void buildsGraphRequestFromStrongPassageCandidates() {
        UUID strongId = UUID.randomUUID();
        UUID relatedId = UUID.randomUUID();
        UUID ignoredId = UUID.randomUUID();
        UUID weakId = UUID.randomUUID();
        List<PassageCandidate> candidates = List.of(
                new PassageCandidate(strongId, 0.91),
                new PassageCandidate(relatedId, 0.82),
                new PassageCandidate(ignoredId, 0.0),
                new PassageCandidate(weakId, 0.22)
        );

        when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
        when(semanticSearch.findPassageCandidate(QUERY_VECTOR, userId, 10)).thenReturn(candidates);
        when(semanticSearch.findTripleCandidates(QUERY_VECTOR, userId, 30)).thenReturn(List.of());
        when(knowledgeGraphSearch.expandKnowledge(any(GraphSearchRequest.class))).thenReturn(GraphSearchResult.empty());

        SearchResult result = useCase.execute(new SearchSourceQuery(SEARCH_TERM));

        ArgumentCaptor<GraphSearchRequest> requestCaptor = ArgumentCaptor.forClass(GraphSearchRequest.class);
        verify(semanticSearch).findPassageCandidate(QUERY_VECTOR, userId, 10);
        verify(semanticSearch).findTripleCandidates(QUERY_VECTOR, userId, 30);
        verifyNoInteractions(recognitionMemory);
        verify(knowledgeGraphSearch).expandKnowledge(requestCaptor.capture());
        verify(semanticSearch, never()).hydrateAndRankChunks(any(), any(UUID.class));

        GraphSearchRequest request = requestCaptor.getValue();
        assertThat(request.userId()).isEqualTo(userId);
        assertThat(request.limit()).isEqualTo(20);
        assertThat(request.seeds()).hasSize(2);
        assertThat(request.seeds())
                .extracting(seed -> ((PassageSeedTarget) seed.target()).chunkId())
                .containsExactly(strongId, relatedId);
        assertThat(request.seeds())
                .extracting(seed -> seed.weight())
                .containsExactly(0.91, 0.82);
        assertThat(result).isEqualTo(SearchResult.empty());
    }

    @Test
    @DisplayName("uses recognition memory to transform triple candidates into concept seeds")
    void usesRecognitionMemoryToBuildConceptSeeds() {
        TripleCandidate tripleCandidate = new TripleCandidate(
                "spring-IMPLEMENTS-repository pattern",
                "spring",
                "IMPLEMENTS",
                "repository pattern",
                UUID.randomUUID(),
                0.88
        );

        when(embeddingPort.embed("Spring")).thenReturn(QUERY_VECTOR);
        when(semanticSearch.findPassageCandidate(QUERY_VECTOR, userId, 10)).thenReturn(List.of());
        when(semanticSearch.findTripleCandidates(QUERY_VECTOR, userId, 30)).thenReturn(List.of(tripleCandidate));
        when(recognitionMemory.recognize("Spring", List.of(tripleCandidate), 10))
                .thenReturn(List.of(GraphSeed.concept("spring", 0.88)));
        when(knowledgeGraphSearch.expandKnowledge(any(GraphSearchRequest.class))).thenReturn(GraphSearchResult.empty());

        SearchResult result = useCase.execute(new SearchSourceQuery("Spring"));

        ArgumentCaptor<GraphSearchRequest> requestCaptor = ArgumentCaptor.forClass(GraphSearchRequest.class);
        verify(knowledgeGraphSearch).expandKnowledge(requestCaptor.capture());

        GraphSearchRequest request = requestCaptor.getValue();
        assertThat(request.seeds()).singleElement()
                .satisfies(seed -> {
                    assertThat(((ConceptSeedTarget) seed.target()).concept().name()).isEqualTo("spring");
                    assertThat(seed.weight()).isEqualTo(0.88);
                });
        assertThat(result).isEqualTo(SearchResult.empty());
    }

    @Test
    @DisplayName("filters weak concept seeds returned by recognition memory before graph expansion")
    void filtersWeakRecognizedConceptSeedsBeforeGraphExpansion() {
        TripleCandidate tripleCandidate = new TripleCandidate(
                "spring-IMPLEMENTS-repository pattern",
                "spring",
                "IMPLEMENTS",
                "repository pattern",
                UUID.randomUUID(),
                0.88
        );

        when(embeddingPort.embed("Spring")).thenReturn(QUERY_VECTOR);
        when(semanticSearch.findPassageCandidate(QUERY_VECTOR, userId, 10)).thenReturn(List.of());
        when(semanticSearch.findTripleCandidates(QUERY_VECTOR, userId, 30)).thenReturn(List.of(tripleCandidate));
        when(recognitionMemory.recognize("Spring", List.of(tripleCandidate), 10))
                .thenReturn(List.of(
                        GraphSeed.concept("spring", 0.88),
                        GraphSeed.concept("repository pattern", 0.39)
                ));
        when(knowledgeGraphSearch.expandKnowledge(any(GraphSearchRequest.class))).thenReturn(GraphSearchResult.empty());

        useCase.execute(new SearchSourceQuery("Spring"));

        ArgumentCaptor<GraphSearchRequest> requestCaptor = ArgumentCaptor.forClass(GraphSearchRequest.class);
        verify(knowledgeGraphSearch).expandKnowledge(requestCaptor.capture());

        GraphSearchRequest request = requestCaptor.getValue();
        assertThat(request.seeds()).singleElement()
                .satisfies(seed -> {
                    assertThat(((ConceptSeedTarget) seed.target()).concept().name()).isEqualTo("spring");
                    assertThat(seed.weight()).isEqualTo(0.88);
                });
    }

    @Test
    @DisplayName("keeps passage seeds and appends concept seeds recognized from triples")
    void keepsPassageSeedsAndAppendsRecognizedConceptSeeds() {
        UUID passageId = UUID.randomUUID();
        TripleCandidate tripleCandidate = new TripleCandidate(
                "mind-RELATES_TO-consciousness",
                "mind",
                "RELATES_TO",
                "consciousness",
                UUID.randomUUID(),
                0.91
        );

        when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
        when(semanticSearch.findPassageCandidate(QUERY_VECTOR, userId, 10))
                .thenReturn(List.of(new PassageCandidate(passageId, 0.9)));
        when(semanticSearch.findTripleCandidates(QUERY_VECTOR, userId, 30)).thenReturn(List.of(tripleCandidate));
        when(recognitionMemory.recognize(SEARCH_TERM, List.of(tripleCandidate), 10))
                .thenReturn(List.of(GraphSeed.concept("consciousness", 0.82)));
        when(knowledgeGraphSearch.expandKnowledge(any(GraphSearchRequest.class))).thenReturn(GraphSearchResult.empty());

        useCase.execute(new SearchSourceQuery(SEARCH_TERM));

        ArgumentCaptor<GraphSearchRequest> requestCaptor = ArgumentCaptor.forClass(GraphSearchRequest.class);
        verify(knowledgeGraphSearch).expandKnowledge(requestCaptor.capture());

        GraphSearchRequest request = requestCaptor.getValue();
        assertThat(request.seeds()).hasSize(2);
        assertThat(((PassageSeedTarget) request.seeds().get(0).target()).chunkId()).isEqualTo(passageId);
        assertThat(((ConceptSeedTarget) request.seeds().get(1).target()).concept().name()).isEqualTo("consciousness");
    }

    @Test
    @DisplayName("returns empty result without graph or hydration when no candidates exist")
    void returnsEmptyWhenNoCandidatesExist() {
        when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
        when(semanticSearch.findPassageCandidate(QUERY_VECTOR, userId, 10)).thenReturn(List.of());
        when(semanticSearch.findTripleCandidates(QUERY_VECTOR, userId, 30)).thenReturn(List.of());

        SearchResult result = useCase.execute(new SearchSourceQuery(SEARCH_TERM));

        assertThat(result).isEqualTo(SearchResult.empty());
        verifyNoInteractions(knowledgeGraphSearch);
        verifyNoInteractions(recognitionMemory);
        verify(semanticSearch, never()).hydrateAndRankChunks(any(), any(UUID.class));
    }

    @Test
    @DisplayName("returns empty result without graph or hydration when all candidate scores are non-positive")
    void returnsEmptyWhenNoPositiveCandidateExists() {
        when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
        when(semanticSearch.findPassageCandidate(QUERY_VECTOR, userId, 10)).thenReturn(List.of(
                new PassageCandidate(UUID.randomUUID(), 0.0),
                new PassageCandidate(UUID.randomUUID(), -0.15)
        ));
        when(semanticSearch.findTripleCandidates(QUERY_VECTOR, userId, 30)).thenReturn(List.of());

        SearchResult result = useCase.execute(new SearchSourceQuery(SEARCH_TERM));

        assertThat(result).isEqualTo(SearchResult.empty());
        verifyNoInteractions(knowledgeGraphSearch);
        verify(semanticSearch, never()).hydrateAndRankChunks(any(), any(UUID.class));
    }

    @Test
    @DisplayName("returns activated triples and ranked chunks in graph score order")
    void returnsActivatedTriplesAndRankedChunks() {
        UUID topId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        List<ScoredPassage> scoredPassages = List.of(
                new ScoredPassage(topId, 0.82),
                new ScoredPassage(secondId, 0.41)
        );
        KnowledgeTriple triple = triple(topId);
        List<RankedChunk> rankedChunks = List.of(
                rankedChunk(topId, 1, 0.82),
                rankedChunk(secondId, 2, 0.41)
        );

        when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
        when(semanticSearch.findPassageCandidate(QUERY_VECTOR, userId, 10))
                .thenReturn(List.of(new PassageCandidate(topId, 0.9)));
        when(semanticSearch.findTripleCandidates(QUERY_VECTOR, userId, 30)).thenReturn(List.of());
        when(knowledgeGraphSearch.expandKnowledge(any(GraphSearchRequest.class)))
                .thenReturn(new GraphSearchResult(scoredPassages, List.of(triple)));
        when(semanticSearch.hydrateAndRankChunks(scoredPassages, userId)).thenReturn(rankedChunks);

        SearchResult result = useCase.execute(new SearchSourceQuery(SEARCH_TERM));

        verify(semanticSearch).hydrateAndRankChunks(scoredPassages, userId);
        assertThat(result.knowledgeTriples()).containsExactly(triple);
        assertThat(result.chunks()).containsExactlyElementsOf(rankedChunks);
        assertThat(result.chunks())
                .extracting(RankedChunk::rank)
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("filters activated triples to deduplicated ranked chunks")
    void filtersActivatedTriplesToRankedChunks() {
        UUID keptId = UUID.randomUUID();
        UUID duplicateContentId = UUID.randomUUID();
        UUID outsideId = UUID.randomUUID();
        List<ScoredPassage> scoredPassages = List.of(
                new ScoredPassage(keptId, 0.91),
                new ScoredPassage(duplicateContentId, 0.82)
        );
        KnowledgeTriple keptTriple = triple(keptId);
        KnowledgeTriple duplicateTriple = KnowledgeTriple.create(
                keptTriple.subject().name(),
                keptTriple.predicate(),
                keptTriple.object().name(),
                Passage.fromChunkId(duplicateContentId),
                1.0
        );
        KnowledgeTriple outsideTriple = triple(outsideId);
        List<RankedChunk> rankedChunks = List.of(rankedChunk(keptId, 1, 0.91));

        when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
        when(semanticSearch.findPassageCandidate(QUERY_VECTOR, userId, 10))
                .thenReturn(List.of(new PassageCandidate(keptId, 0.9)));
        when(semanticSearch.findTripleCandidates(QUERY_VECTOR, userId, 30)).thenReturn(List.of());
        when(knowledgeGraphSearch.expandKnowledge(any(GraphSearchRequest.class)))
                .thenReturn(new GraphSearchResult(scoredPassages, List.of(keptTriple, duplicateTriple, outsideTriple)));
        when(semanticSearch.hydrateAndRankChunks(scoredPassages, userId)).thenReturn(rankedChunks);

        SearchResult result = useCase.execute(new SearchSourceQuery(SEARCH_TERM));

        assertThat(result.chunks()).containsExactlyElementsOf(rankedChunks);
        assertThat(result.knowledgeTriples()).containsExactly(keptTriple);
    }

    @Test
    @DisplayName("propagates embedding failures")
    void propagatesEmbeddingFailures() {
        when(embeddingPort.embed(SEARCH_TERM)).thenThrow(new RuntimeException("Embedding unavailable"));

        assertThatThrownBy(() -> useCase.execute(new SearchSourceQuery(SEARCH_TERM)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Embedding unavailable");
    }

    @Test
    @DisplayName("propagates graph expansion failures")
    void propagatesGraphFailures() {
        when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
        when(semanticSearch.findPassageCandidate(QUERY_VECTOR, userId, 10))
                .thenReturn(List.of(new PassageCandidate(UUID.randomUUID(), 0.9)));
        when(semanticSearch.findTripleCandidates(QUERY_VECTOR, userId, 30)).thenReturn(List.of());
        when(knowledgeGraphSearch.expandKnowledge(any(GraphSearchRequest.class)))
                .thenThrow(new RuntimeException("Neo4j connection refused"));

        assertThatThrownBy(() -> useCase.execute(new SearchSourceQuery(SEARCH_TERM)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Neo4j connection refused");
    }

    @Test
    @DisplayName("propagates hydration failures")
    void propagatesHydrationFailures() {
        UUID chunkId = UUID.randomUUID();
        List<ScoredPassage> scoredPassages = List.of(new ScoredPassage(chunkId, 0.72));

        when(embeddingPort.embed(SEARCH_TERM)).thenReturn(QUERY_VECTOR);
        when(semanticSearch.findPassageCandidate(QUERY_VECTOR, userId, 10))
                .thenReturn(List.of(new PassageCandidate(chunkId, 0.9)));
        when(semanticSearch.findTripleCandidates(QUERY_VECTOR, userId, 30)).thenReturn(List.of());
        when(knowledgeGraphSearch.expandKnowledge(any(GraphSearchRequest.class)))
                .thenReturn(new GraphSearchResult(scoredPassages, List.of()));
        when(semanticSearch.hydrateAndRankChunks(scoredPassages, userId))
                .thenThrow(new RuntimeException("Hydration timeout"));

        assertThatThrownBy(() -> useCase.execute(new SearchSourceQuery(SEARCH_TERM)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Hydration timeout");
    }

    private KnowledgeTriple triple(UUID chunkId) {
        return KnowledgeTriple.create("Consciousness", "relates_to", "Mind", Passage.fromChunkId(chunkId), 1.0);
    }

    private RankedChunk rankedChunk(UUID chunkId, int rank, double score) {
        return new RankedChunk(
                new Chunk(chunkId, source, "Chunk " + rank, rank - 1, true, QUERY_VECTOR),
                rank,
                score,
                RetrievalSource.GRAPH
        );
    }
}
