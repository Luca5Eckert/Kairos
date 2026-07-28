package com.kairos.module.context_engine.infrastructure.relational.projection;

public interface SourceProgressProjection {

    String getTitle();
    String getContent();
    int getTotalChunks();
    int getProcessedChunks();


}
