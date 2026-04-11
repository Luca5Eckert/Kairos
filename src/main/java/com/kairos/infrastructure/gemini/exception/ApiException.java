package com.kairos.infrastructure.gemini.exception;

public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }

}
