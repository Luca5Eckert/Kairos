package com.kairos.domain.extractor;

import com.kairos.domain.model.Chunk;

import java.util.List;

public interface ChunkerExtractor {

    List<String> extract(String content, int chunkSize, int overlap);
}
