package com.kairos.module.context_engine.domain.exception;

public class SourceRetryConflictException extends RuntimeException {

    public SourceRetryConflictException(String message) {
        super(message);
    }
}
