package com.kairos.infrastructure.graph;

import com.kairos.domain.model.Concept;
import com.kairos.domain.model.KnowledgeTriple;
import com.kairos.infrastructure.persistence.entity.graph.PassageNode;
import com.kairos.infrastructure.persistence.repository.graph.Neo4jPassageNodeRepository;
import com.kairos.infrastructure.persistence.repository.graph.Neo4jPhraseNodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphStoreAdapterTest {

    @Mock
    private Neo4jPhraseNodeRepository phraseRepository;

    @Mock
    private Neo4jPassageNodeRepository passageRepository;

    @InjectMocks
    private KnowledgeGraphStoreAdapter adapter;

    @Test
    @DisplayName("saveAllForChunk should ensure passage node and merge triple data")
    void saveAllForChunk_shouldEnsurePassageNodeAndMergeTripleData() {
        UUID chunkId = UUID.randomUUID();
        KnowledgeTriple triple = triple("backpropagation", "USES", "chain rule", chunkId);

        when(passageRepository.existsById(chunkId)).thenReturn(false);

        adapter.saveAllForChunk(chunkId, List.of(triple));

        ArgumentCaptor<PassageNode> passageCaptor = ArgumentCaptor.forClass(PassageNode.class);

        verify(passageRepository).existsById(chunkId);
        verify(passageRepository).save(passageCaptor.capture());
        verify(phraseRepository).mergeTriple("backpropagation", "chain rule", "USES", chunkId);
        verify(passageRepository).mergeConceptLink(chunkId, "backpropagation");
        verify(passageRepository).mergeConceptLink(chunkId, "chain rule");

        assertNotNull(passageCaptor.getValue());
        assertEquals(chunkId, passageCaptor.getValue().getChunkId());
    }

    @Test
    @DisplayName("saveAllForChunk should not save passage node when it already exists")
    void saveAllForChunk_shouldNotSavePassageNodeWhenItAlreadyExists() {
        UUID chunkId = UUID.randomUUID();
        KnowledgeTriple triple = triple("a", "REL", "b", chunkId);

        when(passageRepository.existsById(chunkId)).thenReturn(true);

        adapter.saveAllForChunk(chunkId, List.of(triple));

        verify(passageRepository).existsById(chunkId);
        verify(passageRepository, never()).save(any(PassageNode.class));
        verify(phraseRepository).mergeTriple("a", "b", "REL", chunkId);
        verify(passageRepository).mergeConceptLink(chunkId, "a");
        verify(passageRepository).mergeConceptLink(chunkId, "b");
    }

    @Test
    @DisplayName("saveAllForChunk should merge all triples")
    void saveAllForChunk_shouldMergeAllTriples() {
        UUID chunkId = UUID.randomUUID();
        List<KnowledgeTriple> triples = List.of(
                triple("gradient descent", "MINIMIZES", "loss function", chunkId),
                triple("loss function", "MEASURES", "error", chunkId)
        );

        when(passageRepository.existsById(chunkId)).thenReturn(true);

        adapter.saveAllForChunk(chunkId, triples);

        verify(passageRepository).existsById(chunkId);
        verify(passageRepository, never()).save(any(PassageNode.class));
        verify(phraseRepository, times(2)).mergeTriple(anyString(), anyString(), anyString(), eq(chunkId));
        verify(passageRepository, times(4)).mergeConceptLink(eq(chunkId), anyString());
    }

    @Test
    @DisplayName("saveAllForChunk should do nothing when triple list is empty")
    void saveAllForChunk_shouldDoNothingWhenTripleListIsEmpty() {
        UUID chunkId = UUID.randomUUID();

        adapter.saveAllForChunk(chunkId, List.of());

        verifyNoInteractions(phraseRepository);
        verify(passageRepository, never()).existsById(any());
        verify(passageRepository, never()).save(any(PassageNode.class));
        verify(passageRepository, never()).mergeConceptLink(any(), anyString());
    }

    @Test
    @DisplayName("saveAllForChunk should do nothing when triple list is null")
    void saveAllForChunk_shouldDoNothingWhenTripleListIsNull() {
        UUID chunkId = UUID.randomUUID();

        adapter.saveAllForChunk(chunkId, null);

        verifyNoInteractions(phraseRepository);
        verify(passageRepository, never()).existsById(any());
        verify(passageRepository, never()).save(any(PassageNode.class));
        verify(passageRepository, never()).mergeConceptLink(any(), anyString());
    }

    @Test
    @DisplayName("saveAllForChunk should do nothing when chunkId is null")
    void saveAllForChunk_shouldDoNothingWhenChunkIdIsNull() {
        adapter.saveAllForChunk(null, List.of(triple("a", "REL", "b", UUID.randomUUID())));

        verifyNoInteractions(phraseRepository, passageRepository);
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

        when(passageRepository.existsById(chunkA)).thenReturn(false);
        when(passageRepository.existsById(chunkB)).thenReturn(false);

        adapter.save(triples);

        verify(passageRepository).existsById(chunkA);
        verify(passageRepository).existsById(chunkB);
        verify(passageRepository, times(2)).save(any(PassageNode.class));
        verify(phraseRepository, times(3)).mergeTriple(anyString(), anyString(), anyString(), any(UUID.class));
        verify(passageRepository, times(6)).mergeConceptLink(any(UUID.class), anyString());
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

        verifyNoInteractions(phraseRepository);
        verify(passageRepository, never()).existsById(any());
        verify(passageRepository, never()).save(any(PassageNode.class));
        verify(passageRepository, never()).mergeConceptLink(any(), anyString());
    }

    @Test
    @DisplayName("save should do nothing when triple list is empty")
    void save_shouldDoNothingWhenTripleListIsEmpty() {
        adapter.save(List.of());

        verifyNoInteractions(phraseRepository, passageRepository);
    }

    @Test
    @DisplayName("save should do nothing when triple list is null")
    void save_shouldDoNothingWhenTripleListIsNull() {
        adapter.save(null);

        verifyNoInteractions(phraseRepository, passageRepository);
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