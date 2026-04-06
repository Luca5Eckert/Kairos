package com.kairos.domain.model;

import java.util.UUID;

public class Chunk {

    private final UUID id;
    private final UUID sourceId;
    private final String content;
    private final int index;

    public Chunk(UUID sourceId, String content, int index) {
        this.id = UUID.randomUUID();
        this.sourceId = sourceId;
        this.content = content;
        this.index = index;
    }

    public Chunk(UUID id, UUID sourceId, String content, int index) {
        this.id = id;
        this.sourceId = sourceId;
        this.content = content;
        this.index = index;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getContent() {
        return content;
    }

    public int getIndex() {
        return index;
    }

}