package com.kairos.context_engine.domain.port.graph;

import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchResult;

import java.util.List;

public interface KnowledgeGraphSearch {
    List<KnowledgeTriple> expandKnowledge(List<PassageCandidate> semanticAnchors);

    GraphSearchResult expandKnowledge(GraphSearchRequest graphSearchRequest);
}
