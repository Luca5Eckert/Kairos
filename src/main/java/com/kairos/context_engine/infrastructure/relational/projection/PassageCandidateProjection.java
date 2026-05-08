package com.kairos.context_engine.infrastructure.relational.projection;

import java.util.UUID;

public interface PassageCandidateProjection {
    UUID getChunkId();
    Double getDenseScore();
}
