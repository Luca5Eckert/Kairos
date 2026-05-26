package com.kairos.context_engine.infrastructure.graph.adapter;

import com.kairos.context_engine.domain.model.knowledge.Concept;
import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.context_engine.domain.model.knowledge.Passage;
import com.kairos.context_engine.infrastructure.graph.executor.KnowledgeGraphMutationExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphStoreAdapterTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private KnowledgeGraphMutationExecutor mutationExecutor;

    @InjectMocks
    private KnowledgeGraphStoreAdapter adapter;

    @Test
    @DisplayName("saveAllForChunk should merge triple data")
    void saveAllForChunk_shouldEnsurePassageNodeAndMergeTripleData() {
        UUID chunkId = UUID.randomUUID();
        KnowledgeTriple triple = triple("backpropagation", "USES", "chain rule", chunkId);

        adapter.saveAllForChunk(chunkId, USER_ID, List.of(triple));

        verify(mutationExecutor).mergeTriple("backpropagation", "chain rule", "USES", chunkId, USER_ID, 1.0);
    }

    @Test
    @DisplayName("saveAllForChunk should merge triple data")
    void saveAllForChunk_shouldMergeTripleData() {
        UUID chunkId = UUID.randomUUID();
        KnowledgeTriple triple = triple("a", "REL", "b", chunkId);

        adapter.saveAllForChunk(chunkId, USER_ID, List.of(triple));

        verify(mutationExecutor).mergeTriple("a", "b", "REL", chunkId, USER_ID, 1.0);
    }

    @Test
    @DisplayName("saveAllForChunk should forward structural triple weight")
    void saveAllForChunk_shouldForwardStructuralTripleWeight() {
        UUID chunkId = UUID.randomUUID();
        KnowledgeTriple triple = triple("a", "REL", "b", chunkId, 0.42);

        adapter.saveAllForChunk(chunkId, USER_ID, List.of(triple));

        verify(mutationExecutor).mergeTriple("a", "b", "REL", chunkId, USER_ID, 0.42);
    }

    @Test
    @DisplayName("saveAllForChunk should merge all triples")
    void saveAllForChunk_shouldMergeAllTriples() {
        UUID chunkId = UUID.randomUUID();
        List<KnowledgeTriple> triples = List.of(
                triple("gradient descent", "MINIMIZES", "loss function", chunkId),
                triple("loss function", "MEASURES", "error", chunkId)
        );

        adapter.saveAllForChunk(chunkId, USER_ID, triples);

        verify(mutationExecutor, times(2)).mergeTriple(anyString(), anyString(), anyString(), eq(chunkId), eq(USER_ID), anyDouble());
    }

    @Test
    @DisplayName("saveAllForChunk should do nothing when triple list is empty")
    void saveAllForChunk_shouldDoNothingWhenTripleListIsEmpty() {
        UUID chunkId = UUID.randomUUID();

        adapter.saveAllForChunk(chunkId, USER_ID, List.of());

        verifyNoInteractions(mutationExecutor);
    }

    @Test
    @DisplayName("saveAllForChunk should do nothing when triple list is null")
    void saveAllForChunk_shouldDoNothingWhenTripleListIsNull() {
        UUID chunkId = UUID.randomUUID();

        adapter.saveAllForChunk(chunkId, USER_ID, null);

        verifyNoInteractions(mutationExecutor);
    }

    @Test
    @DisplayName("saveAllForChunk should do nothing when chunkId is null")
    void saveAllForChunk_shouldDoNothingWhenChunkIdIsNull() {
        adapter.saveAllForChunk(null, USER_ID, List.of(triple("a", "REL", "b", UUID.randomUUID())));

        verifyNoInteractions(mutationExecutor);
    }

    @Test
    @DisplayName("save should process triples across multiple chunks")
    void save_shouldProcessTriplesAcrossMultipleChunks() {
        UUID chunkA = UUID.randomUUID();
        UUID chunkB = UUID.randomUUID();

        List<KnowledgeTriple> triples = List.of(
                triple("a", "REL", "b", chunkA),
                triple("c", "REL", "d", chunkA),
                triple("e", "REL", "f", chunkB)
        );

        adapter.save(triples, USER_ID);

        verify(mutationExecutor, times(3)).mergeTriple(anyString(), anyString(), anyString(), any(UUID.class), eq(USER_ID), anyDouble());
    }

    @Test
    @DisplayName("save should skip triples with null chunkId")
    void save_shouldSkipTriplesWithNullChunkId() {
        KnowledgeTriple invalidTriple = new KnowledgeTriple(
                new Concept("subject"),
                "REL",
                new Concept("object"),
                null,
                1.0
        );

        adapter.save(List.of(invalidTriple), USER_ID);

        verifyNoInteractions(mutationExecutor);
    }

    @Test
    @DisplayName("save should do nothing when triple list is empty")
    void save_shouldDoNothingWhenTripleListIsEmpty() {
        adapter.save(List.of(), USER_ID);

        verifyNoInteractions(mutationExecutor);
    }

    @Test
    @DisplayName("save should do nothing when triple list is null")
    void save_shouldDoNothingWhenTripleListIsNull() {
        adapter.save(null, USER_ID);

        verifyNoInteractions(mutationExecutor);
    }

    @Test
    @DisplayName("savePassages should merge passages with user scope")
    void savePassages_shouldMergePassagesWithUserScope() {
        UUID chunkId = UUID.randomUUID();

        adapter.savePassages(List.of(Passage.fromChunkId(chunkId)), USER_ID);

        verify(mutationExecutor).mergePassage(chunkId, USER_ID);
    }

    private KnowledgeTriple triple(String subject, String predicate, String object, UUID chunkId) {
        return triple(subject, predicate, object, chunkId, 1.0);
    }

    private KnowledgeTriple triple(String subject, String predicate, String object, UUID chunkId, double weight) {
        return new KnowledgeTriple(
                new Concept(subject),
                predicate,
                new Concept(object),
                Passage.fromChunkId(chunkId),
                weight
        );
    }
}
