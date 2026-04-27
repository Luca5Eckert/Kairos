package com.kairos.context_engine.domain.port.extraction;

import java.util.List;

public interface ChunkerExtractor {

    List<String> extract(String content, int chunkSize, int overlap);
}
