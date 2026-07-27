package com.kairos.module.auth.domain.port;

public interface PasswordEncoderPort {
    String hash(String password);

    boolean matches(String rawPassword, String passwordHash);
}
