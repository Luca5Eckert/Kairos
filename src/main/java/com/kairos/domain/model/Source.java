package com.kairos.domain.model;

import java.util.UUID;

public class Source {

    private final UUID id;

    private String content;

    private float[] embedding;


    public Source(UUID id, String content, float[] embedding) {
        this.id = id;
        this.content = content;
        this.embedding = embedding;
    }
}
