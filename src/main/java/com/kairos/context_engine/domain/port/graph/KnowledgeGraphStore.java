package com.kairos.context_engine.domain.port.graph;

import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.context_engine.domain.model.knowledge.Passage;

import java.util.List;
import java.util.UUID;

public interface KnowledgeGraphStore {

    void save(List<KnowledgeTriple> domainTriples, UUID userId);

    void saveAllForChunk(UUID chunkId, UUID userId, List<KnowledgeTriple> knowledgeTriples);

    void savePassages(List<Passage> passages, UUID userId);
}
