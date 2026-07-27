package com.kairos.module.context_engine.domain.model.content;

import java.util.UUID;

/**
 * Represents a source document that can be processed to extract concepts and relationships.
 */
public class Source {

    private final UUID id;
    private final String title;
    private final String content;
    private final UUID authorId;

    public Source(String title, String content) {
        this(UUID.randomUUID(), title, content, null);
    }

    public Source(UUID id, String title, String content) {
        this(id, title, content, null);
    }

    public Source(UUID id, String title, String content, UUID authorId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
    }

    public static Source create(String title, String content) {
        return new Source(UUID.randomUUID(), title, content);
    }

    public static Source create(String title, String content, UUID authorId) {
        return new Source(UUID.randomUUID(), title, content, authorId);
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

    public UUID getAuthorId() {
        return authorId;
    }

}
