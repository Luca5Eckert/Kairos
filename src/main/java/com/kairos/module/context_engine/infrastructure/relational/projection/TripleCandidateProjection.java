package com.kairos.module.context_engine.infrastructure.relational.projection;

import java.util.UUID;

public interface TripleCandidateProjection {

    String getKey();

    String getSubject();

    String getPredicate();

    String getObject();

    UUID getChunkId();

    Double getSimilarity();
}
