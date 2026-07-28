package com.kairos.module.context_engine.domain.model.progress;

import com.kairos.module.context_engine.domain.model.content.Source;

public record SourceProgressUpload(
        Source source,
        int totalChunks,
        int processedChunks
) {
}
