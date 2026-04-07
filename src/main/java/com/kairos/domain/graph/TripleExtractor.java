package com.kairos.domain.graph;

import com.kairos.domain.model.Triple;

import java.util.List;

public interface TripleExtractor {

    List<Triple> extract(String content);

}
