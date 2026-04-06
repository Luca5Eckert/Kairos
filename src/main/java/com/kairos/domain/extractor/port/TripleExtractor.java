package com.kairos.domain.extractor.port;

import com.kairos.domain.extractor.info.Triple;

import java.util.List;

public interface TripleExtractor {

    List<Triple> extract(String chunkText);

}
