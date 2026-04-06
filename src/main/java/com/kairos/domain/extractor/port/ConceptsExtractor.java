package com.kairos.domain.extractor.port;

import com.kairos.domain.extractor.info.Triple;

import java.util.List;

public interface ConceptsExtractor {

    List<Triple> extract(String chunkText);

}
