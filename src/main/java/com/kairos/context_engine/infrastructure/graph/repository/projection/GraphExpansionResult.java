package com.kairos.context_engine.infrastructure.graph.repository.projection;

/**
 * Projection for HippoRAG PPR graph expansion results.
 * Score reflects the Personalized PageRank value accumulated
 * by the passage containing the subject node. Weight is the
 * structural confidence stored on the TRIPLE relationship.
 */
public interface GraphExpansionResult {
    String subject();
    String predicate();
    String object();
    String chunkId();
    double score();
    double weight();
}
