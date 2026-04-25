package com.kairos.user.infrastructure.auth;

import com.kairos.auth.domain.exception.AuthenticationDomainException;
import com.kairos.auth.domain.model.PendingUser;
import com.kairos.auth.domain.port.PasswordEncoderPort;
import com.kairos.user.domain.model.Role;
import com.kairos.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.user.infrastructure.persistence.repository.UserEntityJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
                .id(1L)
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
        assertThat(result.id()).isEqualTo(1L);
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
}
