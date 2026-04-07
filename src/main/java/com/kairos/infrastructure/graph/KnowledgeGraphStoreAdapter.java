package com.kairos.infrastructure.graph;

import com.kairos.domain.model.KnowledgeTriple;
import com.kairos.domain.port.KnowledgeGraphStore;
import com.kairos.infrastructure.persistence.entity.graph.PassageNode;
import com.kairos.infrastructure.persistence.repository.graph.Neo4jPassageNodeRepository;
import com.kairos.infrastructure.persistence.repository.graph.Neo4jPhraseNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeGraphStoreAdapter implements KnowledgeGraphStore {

    private final Neo4jPhraseNodeRepository phraseRepository;
    private final Neo4jPassageNodeRepository passageRepository;

    @Override
    @Transactional
    public void save(UUID chunkId, List<KnowledgeTriple> triples) {
        if (triples == null || triples.isEmpty()) {
            log.warn("No triples to save for chunkId: {}", chunkId);
            return;
        }

        ensurePassageNode(chunkId);

        for (KnowledgeTriple triple : triples) {
            String subject   = triple.subject().name();
            String object    = triple.object().name();
            String predicate = triple.predicate();

            phraseRepository.mergeTriple(subject, object, predicate, chunkId);
            passageRepository.mergeConceptLink(chunkId, subject);
            passageRepository.mergeConceptLink(chunkId, object);
        }

        log.info("Merged {} triples and linked concepts to chunk {}", triples.size(), chunkId);
    }

    private void ensurePassageNode(UUID chunkId) {
        if (!passageRepository.existsById(chunkId)) {
            passageRepository.save(PassageNode.forChunk(chunkId));
        }
    }
}