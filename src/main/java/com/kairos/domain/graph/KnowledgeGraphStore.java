package com.kairos.domain.graph;

import com.kairos.domain.model.KnowledgeTriple;

import java.util.List;
import java.util.UUID;

public interface KnowledgeGraphStore {

    void save(List<KnowledgeTriple> domainTriples);

    void saveAllForChunk(UUID chunkId, List<KnowledgeTriple> knowledgeTriples);
}
