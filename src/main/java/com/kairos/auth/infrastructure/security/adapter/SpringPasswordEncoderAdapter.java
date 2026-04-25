package com.kairos.auth.infrastructure.security.adapter;

import com.kairos.auth.domain.port.PasswordEncoderPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SpringPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    public SpringPasswordEncoderAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null) return false;

        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
