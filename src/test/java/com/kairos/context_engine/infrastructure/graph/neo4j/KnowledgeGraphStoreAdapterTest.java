package com.kairos.context_engine.infrastructure.graph.neo4j;

import com.kairos.context_engine.domain.model.Concept;
import com.kairos.context_engine.domain.model.KnowledgeTriple;
import com.kairos.context_engine.infrastructure.graph.neo4j.KnowledgeGraphMutationExecutor;
import com.kairos.context_engine.infrastructure.graph.neo4j.KnowledgeGraphStoreAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphStoreAdapterTest {

    @Mock
    private KnowledgeGraphMutationExecutor mutationExecutor;

    @InjectMocks
    private KnowledgeGraphStoreAdapter adapter;

    @Test
    @DisplayName("saveAllForChunk should merge triple data")
    void saveAllForChunk_shouldEnsurePassageNodeAndMergeTripleData() {
        UUID chunkId = UUID.randomUUID();
        KnowledgeTriple triple = triple("backpropagation", "USES", "chain rule", chunkId);

        adapter.saveAllForChunk(chunkId, List.of(triple));

        verify(mutationExecutor).mergeTriple("backpropagation", "chain rule", "USES", chunkId);
    }

    @Test
    @DisplayName("saveAllForChunk should merge triple data")
    void saveAllForChunk_shouldMergeTripleData() {
        UUID chunkId = UUID.randomUUID();
        KnowledgeTriple triple = triple("a", "REL", "b", chunkId);

        adapter.saveAllForChunk(chunkId, List.of(triple));

        verify(mutationExecutor).mergeTriple("a", "b", "REL", chunkId);
    }

    @Test
    @DisplayName("saveAllForChunk should merge all triples")
    void saveAllForChunk_shouldMergeAllTriples() {
        UUID chunkId = UUID.randomUUID();
        List<KnowledgeTriple> triples = List.of(
                triple("gradient descent", "MINIMIZES", "loss function", chunkId),
                triple("loss function", "MEASURES", "error", chunkId)
        );

        adapter.saveAllForChunk(chunkId, triples);

        verify(mutationExecutor, times(2)).mergeTriple(anyString(), anyString(), anyString(), eq(chunkId));
    }

    @Test
    @DisplayName("saveAllForChunk should do nothing when triple list is empty")
    void saveAllForChunk_shouldDoNothingWhenTripleListIsEmpty() {
        UUID chunkId = UUID.randomUUID();

        adapter.saveAllForChunk(chunkId, List.of());

        verifyNoInteractions(mutationExecutor);
    }

    @Test
    @DisplayName("saveAllForChunk should do nothing when triple list is null")
    void saveAllForChunk_shouldDoNothingWhenTripleListIsNull() {
        UUID chunkId = UUID.randomUUID();

        adapter.saveAllForChunk(chunkId, null);

        verifyNoInteractions(mutationExecutor);
    }

    @Test
    @DisplayName("saveAllForChunk should do nothing when chunkId is null")
    void saveAllForChunk_shouldDoNothingWhenChunkIdIsNull() {
        adapter.saveAllForChunk(null, List.of(triple("a", "REL", "b", UUID.randomUUID())));

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

        adapter.save(triples);

        verify(mutationExecutor, times(3)).mergeTriple(anyString(), anyString(), anyString(), any(UUID.class));
    }

    @Test
    @DisplayName("save should skip triples with null chunkId")
    void save_shouldSkipTriplesWithNullChunkId() {
        KnowledgeTriple invalidTriple = new KnowledgeTriple(
                new Concept("subject", 0, 0),
                "REL",
                new Concept("object", 0, 0),
                null
        );

        adapter.save(List.of(invalidTriple));

        verifyNoInteractions(mutationExecutor);
    }

    @Test
    @DisplayName("save should do nothing when triple list is empty")
    void save_shouldDoNothingWhenTripleListIsEmpty() {
        adapter.save(List.of());

        verifyNoInteractions(mutationExecutor);
    }

    @Test
    @DisplayName("save should do nothing when triple list is null")
    void save_shouldDoNothingWhenTripleListIsNull() {
        adapter.save(null);

        verifyNoInteractions(mutationExecutor);
    }

    private KnowledgeTriple triple(String subject, String predicate, String object, UUID chunkId) {
        return new KnowledgeTriple(
                new Concept(subject, 0, 0),
                predicate,
                new Concept(object, 0, 0),
                chunkId
        );
    }
}
