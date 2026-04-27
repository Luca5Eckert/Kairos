package com.kairos.context_engine.domain.port.graph;

import com.kairos.context_engine.domain.model.Chunk;
import com.kairos.context_engine.domain.model.KnowledgeTriple;

import java.util.List;

public interface KnowledgeGraphSearch {
    List<KnowledgeTriple> expandKnowledge(List<Chunk> semanticAnchors);
}
