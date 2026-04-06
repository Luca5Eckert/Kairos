package com.kairos.domain.model;

import java.util.UUID;

public class Relationship {

    private final UUID sourceId;
    private final UUID targetId;
    private final String predicate;

    public Relationship(UUID sourceId, UUID targetId, String predicate) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.predicate = predicate;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getPredicate() {
        return predicate;
    }
}