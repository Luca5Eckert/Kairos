package com.kairos.module.context_engine.domain.model.content;

import java.util.UUID;

/**
 * Represents a chunk of text extracted from a source document.
 */
public class Chunk {

    private final UUID id;

    private final Source source;
    private final String content;

    private final int index;
    private ChunkProcessingStatus processingStatus;

    private float[] embedding;

    public Chunk(UUID id, Source source, String content, int index, boolean processed, float[] embedding) {
        this(id, source, content, index,
                processed ? ChunkProcessingStatus.COMPLETED : ChunkProcessingStatus.PENDING,
                embedding);
    }

    public Chunk(UUID id, Source source, String content, int index, ChunkProcessingStatus processingStatus, float[] embedding) {
        this.id = id;
        this.source = source;
        this.content = content;
        this.index = index;
        this.processingStatus = processingStatus;
        this.embedding = embedding;
    }

    public Chunk(UUID id, Source source, String content, int index, boolean processed) {
        this(id, source, content, index, processed, null);
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
        this.processingStatus = ChunkProcessingStatus.COMPLETED;
    }

    public void markAsProcessing() {
        this.processingStatus = ChunkProcessingStatus.PROCESSING;
    }

    public void markAsFailed() {
        this.processingStatus = ChunkProcessingStatus.FAILED;
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
        return processingStatus == ChunkProcessingStatus.COMPLETED;
    }

    public void setProcessed(boolean processed) {
        this.processingStatus = processed ? ChunkProcessingStatus.COMPLETED : ChunkProcessingStatus.PENDING;
    }

    public ChunkProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
