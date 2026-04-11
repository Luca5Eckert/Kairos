package com.kairos.domain.model;

import java.util.UUID;

/**
 * Represents a source document that can be processed to extract concepts and relationships.
 */
public class Source {

    private final UUID id;
    private final String title;
    private final String content;
    private SourceStatus status;

    public Source(String title, String content, SourceStatus status) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.content = content;
        this.status = SourceStatus.PENDING;
    }

    public Source(UUID id, String title, String content, SourceStatus status) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.status = status;
    }

    public static Source create(String title, String content) {
        return new Source(UUID.randomUUID(), title, content, SourceStatus.PENDING);
    }

    public void markProcessing() {
        this.status = SourceStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = SourceStatus.COMPLETED;
    }

    public void markPartialFailure() {
        this.status = SourceStatus.PARTIAL_FAILURE;
    }

    public void markFailed() {
        this.status = SourceStatus.FAILED;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public SourceStatus getStatus() {
        return status;
    }

}