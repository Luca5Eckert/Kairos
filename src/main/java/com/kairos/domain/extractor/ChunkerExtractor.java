package com.kairos.domain.extractor;

import java.util.List;

public interface ChunkerExtractor {

    List<String> extract(String content, int chunkSize, int overlap);
}
