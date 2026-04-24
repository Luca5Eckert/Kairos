package com.kairos.auth.domain.port;

public interface PasswordEncoderPort {
    String hash(String password);
}
