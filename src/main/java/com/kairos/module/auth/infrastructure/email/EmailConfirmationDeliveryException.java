package com.kairos.module.auth.infrastructure.email;

public class EmailConfirmationDeliveryException extends RuntimeException {

    public EmailConfirmationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
