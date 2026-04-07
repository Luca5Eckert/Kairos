package com.kairos.domain.port;

import com.kairos.domain.model.KnowledgeTriple;

import java.util.List;
import java.util.UUID;

public interface KnowledgeGraphStore {

    void save(UUID chunkId, List<KnowledgeTriple> domainTriples);

}
