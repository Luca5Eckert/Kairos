package com.kairos.context_engine.infrastructure.relational.repository.projection;

import java.util.UUID;

public interface PassageCandidateProjection {
    UUID getChunkId();
    double getDenseScore();
}