package com.kairos.context_engine.domain.model.content;

public class TripleExtracted {

    private final String key;

    private final String suject;
    private final String predicate;
    private final String object;

    private final Chunk chunk;

    protected TripleExtracted(String key, String suject, String predicate, String object, Chunk chunk) {
        this.key = key;
        this.suject = suject;
        this.predicate = predicate;
        this.object = object;
        this.chunk = chunk;
    }

    public static TripleExtracted create(String suject, String predicate, String object, Chunk chunk) {
        String key = suject + "-" + predicate + "-" + object;
        return new TripleExtracted(key, suject, predicate, object, chunk);
    }

    public String getKey() {
        return key;
    }

    public String getSuject() {
        return suject;
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
