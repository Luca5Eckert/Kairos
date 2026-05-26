package com.kairos.share.infrastructure.persistence.bootstrap;

import com.kairos.user.domain.model.Role;
import com.kairos.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.user.infrastructure.persistence.repository.UserEntityJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DockerAdminBootstrap")
class DockerAdminBootstrapTest {

    @Mock
    private UserEntityJpaRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("creates confirmed admin when enabled and user does not exist")
    void createsConfirmedAdminWhenEnabledAndUserDoesNotExist() {
        when(users.existsByEmailIgnoreCase("admin@kairos.local")).thenReturn(false);
        when(users.existsByUsernameIgnoreCase("admin")).thenReturn(false);
        when(passwordEncoder.encode("Admin123!")).thenReturn("hashed-password");

        bootstrap(true).run(null);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).save(userCaptor.capture());

        UserEntity admin = userCaptor.getValue();
        assertThat(admin.getName()).isEqualTo("Kairos Admin");
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getEmail()).isEqualTo("admin@kairos.local");
        assertThat(admin.getHashPassword()).isEqualTo("hashed-password");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isEmailConfirmed()).isTrue();
        assertThat(admin.getConfirmationCodeHash()).isNull();
    }

    @Test
    @DisplayName("does not create admin when bootstrap is disabled")
    void doesNotCreateAdminWhenBootstrapIsDisabled() {
        bootstrap(false).run(null);

        verifyNoInteractions(users, passwordEncoder);
    }

    @Test
    @DisplayName("does not create admin when email already exists")
    void doesNotCreateAdminWhenEmailAlreadyExists() {
        when(users.existsByEmailIgnoreCase("admin@kairos.local")).thenReturn(true);

        bootstrap(true).run(null);

        verify(users, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("does not create admin when username already exists")
    void doesNotCreateAdminWhenUsernameAlreadyExists() {
        when(users.existsByEmailIgnoreCase("admin@kairos.local")).thenReturn(false);
        when(users.existsByUsernameIgnoreCase("admin")).thenReturn(true);

        bootstrap(true).run(null);

        verify(users, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    private DockerAdminBootstrap bootstrap(boolean enabled) {
        return new DockerAdminBootstrap(
                users,
                passwordEncoder,
                enabled,
                "Kairos Admin",
                "admin",
                "admin@kairos.local",
                "Admin123!"
        );
    }
}
