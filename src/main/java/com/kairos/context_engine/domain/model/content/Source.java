package com.kairos.context_engine.domain.model.content;

import java.util.UUID;

/**
 * Represents a source document that can be processed to extract concepts and relationships.
 */
public class Source {

    private final UUID id;
    private final String title;
    private final String content;

    public Source(String title, String content) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.content = content;
    }

    public Source(UUID id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public static Source create(String title, String content) {
        return new Source(UUID.randomUUID(), title, content);
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

}
