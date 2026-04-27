package com.kairos.context_engine.domain.port.graph;

import com.kairos.context_engine.domain.model.Chunk;
import com.kairos.context_engine.domain.model.KnowledgeTriple;

import java.util.List;
import java.util.UUID;

public interface KnowledgeGraphStore {

    void save(List<KnowledgeTriple> domainTriples);

    void saveAllForChunk(UUID chunkId, List<KnowledgeTriple> knowledgeTriples);

    void createContext(List<Chunk> chunks);
}
