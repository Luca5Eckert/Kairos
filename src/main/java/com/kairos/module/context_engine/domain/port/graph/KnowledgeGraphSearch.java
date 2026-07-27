package com.kairos.module.context_engine.domain.port.graph;

import com.kairos.module.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.module.context_engine.domain.model.retrieval.graph.GraphSearchResult;

public interface KnowledgeGraphSearch {

    GraphSearchResult expandKnowledge(GraphSearchRequest graphSearchRequest);

}
