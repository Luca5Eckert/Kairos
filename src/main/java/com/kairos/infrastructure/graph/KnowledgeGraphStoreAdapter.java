package com.kairos.infrastructure.graph;

import com.kairos.domain.model.KnowledgeTriple;
import com.kairos.domain.graph.KnowledgeGraphStore;
import com.kairos.infrastructure.persistence.entity.graph.PassageNode;
import com.kairos.infrastructure.persistence.repository.graph.Neo4jPassageNodeRepository;
import com.kairos.infrastructure.persistence.repository.graph.Neo4jPhraseNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeGraphStoreAdapter implements KnowledgeGraphStore {

    private final Neo4jPhraseNodeRepository phraseRepository;
    private final Neo4jPassageNodeRepository passageRepository;

    @Override
    @Transactional
    public void save(List<KnowledgeTriple> triples) {
        if (triples == null || triples.isEmpty()) {
            log.debug("No triples to save.");
            return;
        }

        Set<UUID> ensuredChunks = new java.util.HashSet<>();

        for (KnowledgeTriple triple : triples) {
            UUID chunkId = triple.chunkId();

            if (chunkId == null) {
                log.error("Triple for subject '{}' has no chunkId. Skipping.", triple.subject().name());
                continue;
            }

            if (ensuredChunks.add(chunkId)) {
                ensurePassageNode(chunkId);
            }

            mergeTriple(triple, chunkId);
        }

        log.info("Successfully processed {} triples across {} different chunks.", triples.size(), ensuredChunks.size());
    }

    @Override
    @Transactional
    public void saveAllForChunk(UUID chunkId, List<KnowledgeTriple> triples) {
        if (chunkId == null) {
            log.error("Cannot save triples because chunkId is null.");
            return;
        }

        if (triples == null || triples.isEmpty()) {
            log.warn("No triples to save for chunkId: {}", chunkId);
            return;
        }

        ensurePassageNode(chunkId);
        triples.forEach(triple -> mergeTriple(triple, chunkId));

        log.info("Successfully processed {} triples for chunk {}.", triples.size(), chunkId);
    }

    private void mergeTriple(KnowledgeTriple triple, UUID chunkId) {
        phraseRepository.mergeTriple(triple.subject().name(), triple.object().name(), triple.predicate(), chunkId);
        passageRepository.mergeConceptLink(chunkId, triple.subject().name());
        passageRepository.mergeConceptLink(chunkId, triple.object().name());
    }

    private void ensurePassageNode(UUID chunkId) {
        if (chunkId == null) {
            log.error("Cannot ensure PassageNode because chunkId is null.");
            return;
        }

        if (!passageRepository.existsById(chunkId)) {
            log.debug("Creating new PassageNode for chunk: {}", chunkId);
            passageRepository.save(PassageNode.forChunk(chunkId));
        }
    }
}
