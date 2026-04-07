package com.kairos.domain.graph;

import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.KnowledgeTriple;

import java.util.List;

public interface KnowledgeGraphSearch {
    List<KnowledgeTriple> expandKnowledge(List<Chunk> semanticAnchors);
}
