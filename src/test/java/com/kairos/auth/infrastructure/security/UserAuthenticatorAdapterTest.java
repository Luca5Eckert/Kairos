package com.kairos.auth.infrastructure.security;

import com.kairos.auth.domain.exception.AuthenticationDomainException;
import com.kairos.auth.domain.port.PasswordEncoderPort;
import com.kairos.auth.infrastructure.security.adapter.UserAuthenticatorAdapter;
import com.kairos.user.domain.model.Role;
import com.kairos.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.user.infrastructure.persistence.repository.UserEntityJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthenticatorAdapterTest {

    @Mock private UserEntityJpaRepository users;
    @Mock private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private UserAuthenticatorAdapter adapter;

    @Test
    @DisplayName("authenticate - returns authenticated user when credentials match")
    void authenticate_validCredentials_returnsAuthenticatedUser() {
        UserEntity user = confirmedUser();

        when(users.findByEmailIgnoreCaseOrUsernameIgnoreCase("lucas@example.com", "lucas@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("RawPassword123!", "hashed-password")).thenReturn(true);

        var result = adapter.authenticate("lucas@example.com", "RawPassword123!");

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("lucas@example.com");
        assertThat(result.roles()).containsExactly(Role.FREE);
    }

    @Test
    @DisplayName("authenticate - rejects unconfirmed users before password comparison")
    void authenticate_unconfirmedUser_rejectsWithoutCheckingPassword() {
        UserEntity user = confirmedUser();
        user.setEmailConfirmed(false);

        when(users.findByEmailIgnoreCaseOrUsernameIgnoreCase("lucas@example.com", "lucas@example.com"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adapter.authenticate("lucas@example.com", "RawPassword123!"))
                .isInstanceOf(AuthenticationDomainException.class)
                .hasMessage("Email is not confirmed");

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("authenticate - rejects invalid password")
    void authenticate_invalidPassword_rejects() {
        UserEntity user = confirmedUser();

        when(users.findByEmailIgnoreCaseOrUsernameIgnoreCase("lucas@example.com", "lucas@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> adapter.authenticate("lucas@example.com", "wrong-password"))
                .isInstanceOf(AuthenticationDomainException.class)
                .hasMessage("Invalid credentials");
    }

    private UserEntity confirmedUser() {
        return UserEntity.builder()
                .id(1L)
                .name("Lucas")
                .username("lucas")
                .email("lucas@example.com")
                .hashPassword("hashed-password")
                .role(Role.FREE)
                .emailConfirmed(true)
                .build();
    }
}
