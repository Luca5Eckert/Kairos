package com.kairos.module.user.infrastructure.auth;

import com.kairos.module.auth.domain.exception.AuthenticationDomainException;
import com.kairos.module.auth.domain.model.PendingUser;
import com.kairos.module.auth.domain.port.PasswordEncoderPort;
import com.kairos.module.user.domain.model.Role;
import com.kairos.module.user.infrastructure.auth.UserRegistrationAdapter;
import com.kairos.module.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.module.user.infrastructure.persistence.repository.UserEntityJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationAdapterTest {

    @Mock private UserEntityJpaRepository users;
    @Mock private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private UserRegistrationAdapter adapter;

    @Test
    @DisplayName("ensureEmailIsAvailable - rejects an email that is already registered")
    void ensureEmailIsAvailable_registeredEmail_rejects() {
        when(users.existsByEmailIgnoreCase("lucas@example.com")).thenReturn(true);

        assertThatThrownBy(() -> adapter.ensureEmailIsAvailable("lucas@example.com"))
                .isInstanceOf(AuthenticationDomainException.class)
                .hasMessage("Email is already in use");
    }

    @Test
    @DisplayName("ensureUsernameIsAvailable - rejects a username that is already registered")
    void ensureUsernameIsAvailable_registeredUsername_rejects() {
        when(users.existsByUsernameIgnoreCase("lucas")).thenReturn(true);

        assertThatThrownBy(() -> adapter.ensureUsernameIsAvailable("lucas"))
                .isInstanceOf(AuthenticationDomainException.class)
                .hasMessage("Username is already in use");
    }

    @Test
    @DisplayName("savePending - persists unconfirmed free user with confirmation code")
    void savePending_persistsUnconfirmedUser() {
        var pendingUser = PendingUser.create("Lucas", "lucas", "lucas@example.com", "hashed-password");

        when(passwordEncoder.hash("123456")).thenReturn("hashed-confirmation-code");

        adapter.savePending(pendingUser, "123456");

        var captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).save(captor.capture());

        UserEntity entity = captor.getValue();
        assertThat(entity.getName()).isEqualTo("Lucas");
        assertThat(entity.getUsername()).isEqualTo("lucas");
        assertThat(entity.getEmail()).isEqualTo("lucas@example.com");
        assertThat(entity.getHashPassword()).isEqualTo("hashed-password");
        assertThat(entity.getRole()).isEqualTo(Role.FREE);
        assertThat(entity.isEmailConfirmed()).isFalse();
        assertThat(entity.getConfirmationCodeHash()).isEqualTo("hashed-confirmation-code");
    }

    @Test
    @DisplayName("confirmEmail - activates user and clears confirmation code")
    void confirmEmail_validCode_activatesUser() {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .name("Lucas")
                .username("lucas")
                .email("lucas@example.com")
                .hashPassword("hashed-password")
                .role(Role.FREE)
                .emailConfirmed(false)
                .confirmationCodeHash("hashed-confirmation-code")
                .build();

        when(users.findByEmailIgnoreCase("lucas@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hashed-confirmation-code")).thenReturn(true);
        when(users.save(user)).thenReturn(user);

        var result = adapter.confirmEmail("lucas@example.com", "123456");

        assertThat(user.isEmailConfirmed()).isTrue();
        assertThat(user.getConfirmationCodeHash()).isNull();
        assertThat(result.roles()).containsExactly(Role.FREE);
    }

    @Test
    @DisplayName("confirmEmail - rejects invalid code")
    void confirmEmail_invalidCode_rejects() {
        UserEntity user = UserEntity.builder()
                .email("lucas@example.com")
                .emailConfirmed(false)
                .confirmationCodeHash("hashed-confirmation-code")
                .build();

        when(users.findByEmailIgnoreCase("lucas@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("999999", "hashed-confirmation-code")).thenReturn(false);

        assertThatThrownBy(() -> adapter.confirmEmail("lucas@example.com", "999999"))
                .isInstanceOf(AuthenticationDomainException.class)
                .hasMessage("Confirmation code is invalid");
    }

    @Test
    @DisplayName("confirmEmail - rejects confirmation for an unknown email")
    void confirmEmail_unknownEmail_rejects() {
        when(users.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.confirmEmail("unknown@example.com", "123456"))
                .isInstanceOf(AuthenticationDomainException.class)
                .hasMessage("Confirmation code is invalid");
    }

    @Test
    @DisplayName("confirmEmail - rejects a user that has already confirmed their email")
    void confirmEmail_alreadyConfirmed_rejects() {
        UserEntity user = UserEntity.builder()
                .email("lucas@example.com")
                .emailConfirmed(true)
                .build();

        when(users.findByEmailIgnoreCase("lucas@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adapter.confirmEmail("lucas@example.com", "123456"))
                .isInstanceOf(AuthenticationDomainException.class)
                .hasMessage("Email is already confirmed");
    }
}
