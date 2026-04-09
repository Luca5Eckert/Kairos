package com.kairos.infrastructure.gemini.exception;

public class ApiException extends RuntimeException {
    private int code;

    public ApiException(String message) {
        super(message);
    }

    public int code() {
        return code;
    }
}
