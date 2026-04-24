package com.kairos.context_engine.domain.model;

/**
 * Enum representing the status of a source in the system.
 */
public enum SourceStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    PARTIAL_FAILURE,
    FAILED
}