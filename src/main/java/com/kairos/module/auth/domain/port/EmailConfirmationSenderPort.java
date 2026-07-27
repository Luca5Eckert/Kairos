package com.kairos.module.auth.domain.port;

public interface EmailConfirmationSenderPort {
    void send(String code, String email);
}
