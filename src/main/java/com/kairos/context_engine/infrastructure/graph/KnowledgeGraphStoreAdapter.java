package com.kairos.context_engine.infrastructure.graph;

import com.kairos.context_engine.domain.model.Chunk;
import com.kairos.context_engine.domain.model.KnowledgeTriple;
import com.kairos.context_engine.domain.port.graph.KnowledgeGraphStore;
import com.kairos.context_engine.infrastructure.persistence.entity.graph.PassageNode;
import com.kairos.context_engine.infrastructure.persistence.repository.graph.Neo4jPassageNodeRepository;
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

    private final KnowledgeGraphMutationExecutor mutationExecutor;
    private final Neo4jPassageNodeRepository passageNodeRepository;

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

            ensuredChunks.add(chunkId);

            mergeTriple(triple, chunkId);
        }

        log.info("Successfully processed {} triples across {} different chunks.", triples.size(), ensuredChunks.size());
    }

    @Override
    @Transactional
    public void saveAllForChunk(UUID chunkId, List<KnowledgeTriple> triples) {
        if (chunkId == null) {
            log.error("Attempted to save triples with null chunkId. This indicates a bug in the calling flow.");
            return;
        }

        if (triples == null || triples.isEmpty()) {
            log.warn("No triples to save for chunkId: {}", chunkId);
            return;
        }

        triples.forEach(triple -> mergeTriple(triple, chunkId));

        log.info("Successfully processed {} triples for chunk {}.", triples.size(), chunkId);
    }

    @Override
    @Transactional
    public void createContext(List<Chunk> chunks) {
            if (chunks == null || chunks.isEmpty()) {
                log.warn("No chunks provided for context creation.");
                return;
            }

            for (Chunk chunk : chunks) {
                if (chunk.getId() == null) {
                    log.error("Chunk with content '{}' has null ID. Skipping context creation for this chunk.", chunk.getContent());
                    continue;
                }

                PassageNode passageNode = new PassageNode(chunk.getId());
                passageNodeRepository.save(passageNode);
            }

            log.info("Successfully created context for {} chunks.", chunks.size());
    }

    private void mergeTriple(KnowledgeTriple triple, UUID chunkId) {
        mutationExecutor.mergeTriple(triple.subject().name(), triple.object().name(), triple.predicate(), chunkId);
    }
}
