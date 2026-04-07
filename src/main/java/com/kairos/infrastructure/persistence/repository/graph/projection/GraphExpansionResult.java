package com.kairos.infrastructure.persistence.repository.graph.projection;

/**
 * Represents the result of a graph expansion operation, containing the subject, predicate, object, and the associated chunk ID.
 * @param subject The subject node of the triple.
 * @param predicate The predicate (relationship) connecting the subject and object.
 * @param object The object node of the triple.
 * @param chunkId  The ID of the chunk from which this triple was extracted, used for traceability back to the source passage.
 */
public record GraphExpansionResult(String subject, String predicate, String object, String chunkId) {

}
