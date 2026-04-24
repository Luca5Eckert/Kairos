package com.kairos.auth.domain.port;

public interface EmailConfirmationSenderPort {
    void send(String code, String email);
}
