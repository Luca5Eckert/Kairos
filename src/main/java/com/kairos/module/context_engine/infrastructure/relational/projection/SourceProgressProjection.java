package com.kairos.module.context_engine.infrastructure.relational.projection;

public interface SourceProgressProjection {

    java.util.UUID getId();
    String getTitle();
    String getContent();
    java.util.UUID getAuthorId();
    int getTotalChunks();
    int getPendingChunks();
    int getProcessingChunks();
    int getCompletedChunks();
    int getFailedChunks();
}
