package com.kairos.context_engine.infrastructure.ai.gemini.exception;

public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }

}
