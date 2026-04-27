package com.kairos.context_engine.infrastructure.graph.repository.projection;

/**
 * Projection for HippoRAG PPR graph expansion results.
 * Score reflects the Personalized PageRank value accumulated
 * by the passage containing the subject node.
 */
public interface GraphExpansionResult {
    String subject();
    String predicate();
    String object();
    String chunkId();
    double score();
}