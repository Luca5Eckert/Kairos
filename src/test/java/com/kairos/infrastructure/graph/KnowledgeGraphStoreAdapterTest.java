package com.kairos.infrastructure.graph;

import com.kairos.domain.model.Concept;
import com.kairos.domain.model.KnowledgeTriple;
import com.kairos.infrastructure.persistence.entity.graph.PassageNode;
import com.kairos.infrastructure.persistence.repository.graph.Neo4jPassageNodeRepository;
import com.kairos.infrastructure.persistence.repository.graph.Neo4jPhraseNodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphStoreAdapterTest {

    @Mock private Neo4jPhraseNodeRepository phraseRepository;
    @Mock private Neo4jPassageNodeRepository passageRepository;

    @InjectMocks
    private KnowledgeGraphStoreAdapter adapter;

    // -------------------------------------------------------------------------
    // saveAllForChunk
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("saveAllForChunk — ensures PassageNode and merges each triple")
    void saveAllForChunk_validTriples_ensuresPassageNodeAndMergesTriples() {
        UUID chunkId = UUID.randomUUID();
        KnowledgeTriple triple = triple("backpropagation", "USES", "chain rule", chunkId);

        when(passageRepository.existsById(chunkId)).thenReturn(false);

        adapter.saveAllForChunk(chunkId, List.of(triple));

        verify(passageRepository).save(any(PassageNode.class));
        verify(phraseRepository).mergeTriple("backpropagation", "chain rule", "USES", chunkId);
        verify(passageRepository).mergeConceptLink(chunkId, "backpropagation");
        verify(passageRepository).mergeConceptLink(chunkId, "chain rule");
    }

    @Test
    @DisplayName("saveAllForChunk — skips PassageNode creation when it already exists")
    void saveAllForChunk_passageAlreadyExists_doesNotSavePassageNode() {
        UUID chunkId = UUID.randomUUID();

        when(passageRepository.existsById(chunkId)).thenReturn(true);

        adapter.saveAllForChunk(chunkId, List.of(triple("a", "REL", "b", chunkId)));

        verify(passageRepository, never()).save(any(PassageNode.class));
    }

    @Test
    @DisplayName("saveAllForChunk — merges all triples when multiple are provided")
    void saveAllForChunk_multipleTriples_mergesAll() {
        UUID chunkId = UUID.randomUUID();
        var triples = List.of(
                triple("gradient descent", "MINIMIZES", "loss function", chunkId),
                triple("loss function", "MEASURES", "error", chunkId)
        );

        when(passageRepository.existsById(chunkId)).thenReturn(true);

        adapter.saveAllForChunk(chunkId, triples);

        verify(phraseRepository, times(2)).mergeTriple(anyString(), anyString(), anyString(), eq(chunkId));
        verify(passageRepository, times(4)).mergeConceptLink(eq(chunkId), anyString());
    }

    @Test
    @DisplayName("saveAllForChunk — does nothing when triple list is empty")
    void saveAllForChunk_emptyList_noInteractions() {
        UUID chunkId = UUID.randomUUID();

        adapter.saveAllForChunk(chunkId, List.of());

        verifyNoInteractions(phraseRepository);
        verify(passageRepository, never()).existsById(any());
        verify(passageRepository, never()).save(any());
        verify(passageRepository, never()).mergeConceptLink(any(), any());
    }

    @Test
    @DisplayName("saveAllForChunk — does nothing when triple list is null")
    void saveAllForChunk_nullList_noInteractions() {
        UUID chunkId = UUID.randomUUID();

        adapter.saveAllForChunk(chunkId, null);

        verifyNoInteractions(phraseRepository);
        verify(passageRepository, never()).existsById(any());
    }

    // -------------------------------------------------------------------------
    // save (batch with chunkId inside triple)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save — processes triples grouped by chunkId")
    void save_multipleChunks_ensuresPassageNodePerChunk() {
        UUID chunkA = UUID.randomUUID();
        UUID chunkB = UUID.randomUUID();

        var triples = List.of(
                triple("a", "REL", "b", chunkA),
                triple("c", "REL", "d", chunkA),
                triple("e", "REL", "f", chunkB)
        );

        when(passageRepository.existsById(chunkA)).thenReturn(false);
        when(passageRepository.existsById(chunkB)).thenReturn(false);

        adapter.save(triples);

        verify(passageRepository, times(2)).save(any(PassageNode.class));
        verify(phraseRepository, times(3)).mergeTriple(anyString(), anyString(), anyString(), any(UUID.class));
    }

    @Test
    @DisplayName("save — skips triples with null chunkId")
    void save_nullChunkId_skipsTriple() {
        KnowledgeTriple invalidTriple = new KnowledgeTriple(
                new Concept("subject", 0, 0), "REL", new Concept("object", 0, 0), null);

        adapter.save(List.of(invalidTriple));

        verifyNoInteractions(phraseRepository);
        verify(passageRepository, never()).save(any());
        verify(passageRepository, never()).mergeConceptLink(any(), any());
    }

    @Test
    @DisplayName("save — does nothing when triple list is empty")
    void save_emptyList_noInteractions() {
        adapter.save(List.of());

        verifyNoInteractions(phraseRepository, passageRepository);
    }

    @Test
    @DisplayName("save — does nothing when triple list is null")
    void save_nullList_noInteractions() {
        adapter.save(null);

        verifyNoInteractions(phraseRepository, passageRepository);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private KnowledgeTriple triple(String subject, String predicate, String object, UUID chunkId) {
        return new KnowledgeTriple(
                new Concept(subject, 0, 0),
                predicate,
                new Concept(object, 0, 0),
                chunkId
        );
    }
}