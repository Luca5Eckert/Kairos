package com.kairos.module.user.application.use_case;

import com.kairos.module.auth.domain.exception.AuthenticationDomainException;
import com.kairos.module.auth.domain.policy.PasswordPolicy;
import com.kairos.module.auth.domain.port.PasswordEncoderPort;
import com.kairos.module.user.application.command.ChangePasswordCommand;
import com.kairos.module.user.application.command.UpdateProfileCommand;
import com.kairos.module.user.domain.model.Role;
import com.kairos.module.user.domain.model.User;
import com.kairos.module.user.domain.repository.UserRepository;
import com.kairos.share.security.context.RequestContext;
import com.kairos.share.security.context.RequestContextProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSelfServiceUseCaseTest {

    @Mock private UserRepository users;
    @Mock private RequestContextProvider requestContextProvider;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private PasswordPolicy passwordPolicy;

    @Test
    void getProfile_readsOnlyTheAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        when(requestContextProvider.getRequestContext()).thenReturn(context(userId));
        when(users.findById(userId)).thenReturn(Optional.of(user));

        User result = new GetMyProfileUseCase(users, requestContextProvider).execute();

        assertThat(result).isSameAs(user);
        verify(users).findById(userId);
    }

    @Test
    void updateProfile_normalizesUsernameAndPersistsOnlyContextUser() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        when(requestContextProvider.getRequestContext()).thenReturn(context(userId));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(users.existsByUsernameIgnoreCaseAndIdNot("new-name", userId)).thenReturn(false);
        when(users.save(user)).thenReturn(user);

        User result = new UpdateMyProfileUseCase(users, requestContextProvider)
                .execute(new UpdateProfileCommand(" New Name ", " New-Name "));

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getUsername()).isEqualTo("new-name");
        verify(users).findById(userId);
        verify(users).existsByUsernameIgnoreCaseAndIdNot("new-name", userId);
        verify(users).save(user);
    }

    @Test
    void updateProfile_rejectsUsernameConflictWithoutSaving() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        when(requestContextProvider.getRequestContext()).thenReturn(context(userId));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(users.existsByUsernameIgnoreCaseAndIdNot("taken", userId)).thenReturn(true);

        assertThatThrownBy(() -> new UpdateMyProfileUseCase(users, requestContextProvider)
                .execute(new UpdateProfileCommand(null, "Taken")))
                .isInstanceOf(AuthenticationDomainException.class)
                .hasMessage("Username is already in use");

        verify(users, never()).save(any());
    }

    @Test
    void changePassword_requiresCurrentPasswordAndStoresOnlyNewHash() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        when(requestContextProvider.getRequestContext()).thenReturn(context(userId));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123!", "old-hash")).thenReturn(true);
        when(passwordEncoder.hash("NewPassword123!")).thenReturn("new-hash");

        new ChangeMyPasswordUseCase(users, requestContextProvider, passwordEncoder, passwordPolicy)
                .execute(new ChangePasswordCommand("OldPassword123!", "NewPassword123!", "NewPassword123!"));

        assertThat(user.getHashPassword()).isEqualTo("new-hash");
        InOrder order = inOrder(passwordEncoder, passwordPolicy, users);
        order.verify(passwordEncoder).matches("OldPassword123!", "old-hash");
        order.verify(passwordPolicy).validate("NewPassword123!");
        order.verify(passwordEncoder).hash("NewPassword123!");
        order.verify(users).save(user);
    }

    @Test
    void changePassword_rejectsInvalidCurrentPasswordWithoutHashing() {
        UUID userId = UUID.randomUUID();
        when(requestContextProvider.getRequestContext()).thenReturn(context(userId));
        when(users.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> new ChangeMyPasswordUseCase(users, requestContextProvider, passwordEncoder, passwordPolicy)
                .execute(new ChangePasswordCommand("wrong", "NewPassword123!", "NewPassword123!")))
                .isInstanceOf(AuthenticationDomainException.class)
                .hasMessage("Current password is invalid");

        verify(passwordPolicy, never()).validate(any());
        verify(passwordEncoder, never()).hash(any());
        verify(users, never()).save(any());
    }

    private RequestContext context(UUID userId) {
        return new RequestContext(userId, "lucas@example.com", List.of(Role.FREE));
    }

    private User user(UUID userId) {
        return new User.Builder()
                .id(userId)
                .name("Lucas")
                .username("lucas")
                .email("lucas@example.com")
                .hashPassword("old-hash")
                .role(Role.FREE)
                .emailConfirmed(true)
                .confirmationCodeHash("confirmation-hash")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
