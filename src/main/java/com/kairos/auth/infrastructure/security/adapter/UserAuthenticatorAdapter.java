package com.kairos.auth.infrastructure.security.adapter;

import com.kairos.auth.domain.exception.AuthenticationDomainException;
import com.kairos.auth.domain.model.AuthenticatedUser;
import com.kairos.auth.domain.port.AuthenticatorPort;
import com.kairos.auth.domain.port.PasswordEncoderPort;
import com.kairos.user.domain.model.Role;
import com.kairos.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.user.infrastructure.persistence.repository.UserEntityJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserAuthenticatorAdapter implements AuthenticatorPort {

    private final UserEntityJpaRepository users;
    private final PasswordEncoderPort passwordEncoder;

    public UserAuthenticatorAdapter(UserEntityJpaRepository users, PasswordEncoderPort passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthenticatedUser authenticate(String identifier, String rawPassword) {
        UserEntity user = users.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new AuthenticationDomainException("Invalid credentials"));

        if (!user.isEmailConfirmed()) {
            throw new AuthenticationDomainException("Email is not confirmed");
        }

        if (!passwordEncoder.matches(rawPassword, user.getHashPassword())) {
            throw new AuthenticationDomainException("Invalid credentials");
        }

        return toAuthenticatedUser(user);
    }

    private AuthenticatedUser toAuthenticatedUser(UserEntity user) {
        Role role = user.getRole() == null ? Role.FREE : user.getRole();

        return new AuthenticatedUser(user.getId(), user.getEmail(), List.of(role));
    }
}
