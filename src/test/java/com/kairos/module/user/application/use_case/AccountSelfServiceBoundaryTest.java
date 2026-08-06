package com.kairos.module.user.application.use_case;

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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSelfServiceBoundaryTest {

    @Mock private UserRepository users;
    @Mock private RequestContextProvider requestContextProvider;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private PasswordPolicy passwordPolicy;

    @Test
    void updateProfile_requiresAtLeastOneField() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UpdateMyProfileUseCase(users, requestContextProvider)
                        .execute(new UpdateProfileCommand(null, null)))
                .withMessage("At least one profile field is required");

        verifyNoInteractions(requestContextProvider, users);
    }

    @Test
    void updateProfile_rejectsBlankName() {
        UUID userId = UUID.randomUUID();
        when(requestContextProvider.getRequestContext()).thenReturn(context(userId));
        when(users.findById(userId)).thenReturn(Optional.of(user(userId)));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UpdateMyProfileUseCase(users, requestContextProvider)
                        .execute(new UpdateProfileCommand("   ", null)))
                .withMessage("Name is required");

        verify(users, never()).save(any());
    }

    @Test
    void updateProfile_allowsChangingOnlyName() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        when(requestContextProvider.getRequestContext()).thenReturn(context(userId));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);

        User updated = new UpdateMyProfileUseCase(users, requestContextProvider)
                .execute(new UpdateProfileCommand("  Updated Name  ", null));

        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getUsername()).isEqualTo("lucas");
        verify(users, never()).existsByUsernameIgnoreCaseAndIdNot(any(), any());
    }

    @Test
    void changePassword_rejectsMismatchedConfirmationWithoutPersisting() {
        UUID userId = UUID.randomUUID();
        when(requestContextProvider.getRequestContext()).thenReturn(context(userId));
        when(users.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(passwordEncoder.matches("OldPassword123!", "old-hash")).thenReturn(true);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ChangeMyPasswordUseCase(users, requestContextProvider, passwordEncoder, passwordPolicy)
                        .execute(new ChangePasswordCommand("OldPassword123!", "NewPassword123!", "different")))
                .withMessage("New password confirmation does not match");

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
                .build();
    }
}
