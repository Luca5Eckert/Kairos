package com.kairos.module.context_engine.domain.model.progress;

import com.kairos.module.context_engine.domain.model.content.Source;

public record SourceProgressUpload(
        Source source,
        int totalChunks,
        int pendingChunks,
        int processingChunks,
        int completedChunks,
        int failedChunks
) {
    public SourceProgressUpload(Source source, int totalChunks, int processedChunks) {
        this(source, totalChunks, totalChunks - processedChunks, 0, processedChunks, 0);
    }

    public SourceProcessingStatus status() {
        if (processingChunks > 0) {
            return SourceProcessingStatus.PROCESSING;
        }
        if (failedChunks > 0) {
            return SourceProcessingStatus.FAILED;
        }
        if (totalChunks > 0 && completedChunks == totalChunks) {
            return SourceProcessingStatus.COMPLETED;
        }
        return SourceProcessingStatus.PENDING;
    }

    public int processedChunks() {
        return completedChunks;
    }
}
