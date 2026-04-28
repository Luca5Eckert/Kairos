package com.kairos.context_engine.domain.model.content;

import java.util.UUID;

/**
 * Represents a chunk of text extracted from a source document.
 */
public class Chunk {

    private final UUID id;

    private final Source source;
    private final String content;

    private final int index;
    private boolean processed;

    private float[] embedding;

    public Chunk(UUID id, Source source, String content, int index, boolean processed, float[] embedding) {
        this.id = id;
        this.source = source;
        this.content = content;
        this.index = index;
        this.processed = processed;
        this.embedding = embedding;
    }

    public Chunk(UUID id, Source source, String content, int index, boolean processed) {
        this.id = id;
        this.source = source;
        this.content = content;
        this.index = index;
        this.processed = processed;
    }



    public static Chunk create(Source source, String content, int index, float[] embedding) {
        return new Chunk(UUID.randomUUID(), source, content, index, false, embedding);
    }

    public static Chunk create(UUID id, Source source, String content, int index, boolean processed, float[] embedding) {
        return new Chunk(id, source, content, index, processed, embedding);
    }

    public static Chunk create(Source source, String content, int index) {
        return new Chunk(UUID.randomUUID(), source, content, index, false);
    }

    public void addEmbedding(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding cannot be null or empty");
        }
        this.embedding = embedding;
    }

    public void markAsProcessed() {
        this.processed = true;
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

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}