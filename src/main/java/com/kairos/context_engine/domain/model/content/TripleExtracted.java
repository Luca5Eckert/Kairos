package com.kairos.context_engine.domain.model.content;

public class TripleExtracted {

    private String key;

    private String suject;
    private String predicate;
    private String object;

    private Chunk chunk;

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
}
