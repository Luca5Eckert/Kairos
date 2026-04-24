package com.kairos.context_engine.domain.graph;

import com.kairos.context_engine.domain.model.Triple;

import java.util.List;

public interface TripleExtractor {

    List<Triple> extract(String content);

}
