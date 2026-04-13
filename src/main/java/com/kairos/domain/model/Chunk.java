package com.kairos.domain.model;

import java.util.UUID;

/**
 * Represents a chunk of text extracted from a source document.
 */
public class Chunk {

    private final UUID id;
    private final Source source;
    private final String content;
    private final int index;
    private final float[] embedding;
    private final boolean triplesExtracted;

    public Chunk(UUID id, Source source, String content, int index, float[] embedding, boolean triplesExtracted) {
        this.id = id;
        this.source = source;
        this.content = content;
        this.index = index;
        this.embedding = embedding;
        this.triplesExtracted = triplesExtracted;
    }

    public static Chunk create(Source source, String content, int index, float[] embedding) {
        return new Chunk(UUID.randomUUID(), source, content, index, embedding, false);
    }

    public static Chunk create(UUID id, Source source, String content, int index, float[] embedding) {
        return new Chunk(id, source, content, index, embedding, false);
    }

    public Chunk markTriplesExtracted() {
        return new Chunk(id, source, content, index, embedding, true);
    }

    public UUID getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public String getContent() {
        return content;
    }

    public int getIndex() {
        return index;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public boolean isTriplesExtracted() {
        return triplesExtracted;
    }

}
