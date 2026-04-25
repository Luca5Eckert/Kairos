package com.kairos.user.infrastructure.auth;

import com.kairos.auth.domain.exception.AuthenticationDomainException;
import com.kairos.auth.domain.model.AuthenticatedUser;
import com.kairos.auth.domain.model.PendingUser;
import com.kairos.auth.domain.port.PasswordEncoderPort;
import com.kairos.auth.domain.port.UserRegistrationPort;
import com.kairos.user.domain.model.Role;
import com.kairos.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.user.infrastructure.persistence.repository.UserEntityJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserRegistrationAdapter implements UserRegistrationPort {

    private final UserEntityJpaRepository users;
    private final PasswordEncoderPort passwordEncoder;

    public UserRegistrationAdapter(UserEntityJpaRepository users, PasswordEncoderPort passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void ensureEmailIsAvailable(String email) {
        if (users.existsByEmailIgnoreCase(email)) {
            throw new AuthenticationDomainException("Email is already in use");
        }
    }

    @Override
    public void ensureUsernameIsAvailable(String username) {
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new AuthenticationDomainException("Username is already in use");
        }
    }

    @Override
    public void savePending(PendingUser pendingUser, String code) {
        UserEntity user = UserEntity.builder()
                .name(pendingUser.name())
                .username(pendingUser.username())
                .email(pendingUser.email())
                .hashPassword(pendingUser.passwordHash())
                .role(Role.FREE)
                .emailConfirmed(false)
                .confirmationCodeHash(passwordEncoder.hash(code))
                .build();

        users.save(user);
    }

    @Override
    public AuthenticatedUser confirmEmail(String email, String code) {
        UserEntity user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthenticationDomainException("Confirmation code is invalid"));

        if (user.isEmailConfirmed()) {
            throw new AuthenticationDomainException("Email is already confirmed");
        }

        if (!passwordEncoder.matches(code, user.getConfirmationCodeHash())) {
            throw new AuthenticationDomainException("Confirmation code is invalid");
        }

        user.setEmailConfirmed(true);
        user.setConfirmationCodeHash(null);
        UserEntity savedUser = users.save(user);

        Role role = savedUser.getRole() == null ? Role.FREE : savedUser.getRole();

        return new AuthenticatedUser(savedUser.getId(), savedUser.getEmail(), List.of(role));
    }
}
