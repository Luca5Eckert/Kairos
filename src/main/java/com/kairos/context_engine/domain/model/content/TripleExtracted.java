package com.kairos.context_engine.domain.model.content;

public class TripleExtracted {

    private final String key;

    private final String subject;
    private final String predicate;
    private final String object;

    private float[] embedding;

    private final Chunk chunk;

    protected TripleExtracted(String key, String subject, String predicate, String object, Chunk chunk) {
        this.key = key;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.chunk = chunk;
    }

    public static TripleExtracted create(String suject, String predicate, String object, Chunk chunk) {
        String key = suject + "-" + predicate + "-" + object;
        return new TripleExtracted(key, suject, predicate, object, chunk);
    }

    public void addEmbedding(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding cannot be null or empty");
        }
        this.embedding = embedding;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public String getKey() {
        return key;
    }

    public String getSuject() {
        return subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getObject() {
        return object;
    }

    public Chunk getChunk() {
        return chunk;
    }

}
