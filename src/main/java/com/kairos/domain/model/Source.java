package com.kairos.domain.model;

import java.util.UUID;

public class Source {

    private final UUID id;

    private String title;

    private String content;

    private float[] embedding;

    public Source(UUID id, String title, String content, float[] embedding) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.embedding = embedding;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

}
