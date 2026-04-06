package com.kairos.domain.model;

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