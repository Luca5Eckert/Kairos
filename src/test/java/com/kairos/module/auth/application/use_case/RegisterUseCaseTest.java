package com.kairos.module.auth.application.use_case;

import com.kairos.module.auth.application.command.RegisterCommand;
import com.kairos.module.auth.domain.model.PendingUser;
import com.kairos.module.auth.domain.policy.PasswordPolicy;
import com.kairos.module.auth.domain.port.CodeConfirmationPort;
import com.kairos.module.auth.domain.port.EmailConfirmationSenderPort;
import com.kairos.module.auth.domain.port.PasswordEncoderPort;
import com.kairos.module.auth.domain.port.UserRegistrationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUseCaseTest {

    @Mock private UserRegistrationPort users;
    @Mock private PasswordPolicy passwordPolicy;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private CodeConfirmationPort codeConfirmation;
    @Mock private EmailConfirmationSenderPort emailSender;

    @InjectMocks
    private RegisterUseCase useCase;

    @Test
    @DisplayName("execute - creates pending user with hashed password and sends confirmation code")
    void execute_validCommand_createsPendingUserAndSendsCode() {
        var command = new RegisterCommand("Lucas", "lucas", "lucas@example.com", "RawPassword123!");

        when(passwordEncoder.hash(command.password())).thenReturn("hashed-password");
        when(codeConfirmation.generateCode()).thenReturn("123456");

        useCase.execute(command);

        var pendingUserCaptor = ArgumentCaptor.forClass(PendingUser.class);
        verify(users).savePending(pendingUserCaptor.capture(), org.mockito.ArgumentMatchers.eq("123456"));

        PendingUser pendingUser = pendingUserCaptor.getValue();
        assertThat(pendingUser.name()).isEqualTo("Lucas");
        assertThat(pendingUser.username()).isEqualTo("lucas");
        assertThat(pendingUser.email()).isEqualTo("lucas@example.com");
        assertThat(pendingUser.passwordHash()).isEqualTo("hashed-password");

        verify(emailSender).send("123456", "lucas@example.com");
    }

    @Test
    @DisplayName("execute - validates availability and password before persisting pending user")
    void execute_validCommand_validatesBeforePersisting() {
        var command = new RegisterCommand("Lucas", "lucas", "lucas@example.com", "RawPassword123!");

        when(passwordEncoder.hash(command.password())).thenReturn("hashed-password");
        when(codeConfirmation.generateCode()).thenReturn("123456");

        useCase.execute(command);

        var inOrder = inOrder(users, passwordPolicy, passwordEncoder, codeConfirmation, emailSender);
        inOrder.verify(users).ensureEmailIsAvailable("lucas@example.com");
        inOrder.verify(users).ensureUsernameIsAvailable("lucas");
        inOrder.verify(passwordPolicy).validate("RawPassword123!");
        inOrder.verify(passwordEncoder).hash("RawPassword123!");
        inOrder.verify(codeConfirmation).generateCode();
        inOrder.verify(users).savePending(org.mockito.ArgumentMatchers.any(PendingUser.class), org.mockito.ArgumentMatchers.eq("123456"));
        inOrder.verify(emailSender).send("123456", "lucas@example.com");
    }

    @Test
    @DisplayName("execute - stops when email is already registered")
    void execute_duplicateEmail_doesNotContinueRegistration() {
        var command = new RegisterCommand("Lucas", "lucas", "lucas@example.com", "RawPassword123!");
        doThrow(new IllegalStateException("Email is already in use"))
                .when(users).ensureEmailIsAvailable(command.email());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email is already in use");

        verifyNoInteractions(passwordPolicy, passwordEncoder, codeConfirmation, emailSender);
    }

    @Test
    @DisplayName("execute - does not persist a user when password validation fails")
    void execute_invalidPassword_doesNotPersistOrSendConfirmation() {
        var command = new RegisterCommand("Lucas", "lucas", "lucas@example.com", "weak");
        doThrow(new IllegalArgumentException("Password is invalid"))
                .when(passwordPolicy).validate(command.password());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password is invalid");

        verifyNoInteractions(passwordEncoder, codeConfirmation, emailSender);
        org.mockito.Mockito.verify(users, org.mockito.Mockito.never())
                .savePending(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("execute - propagates confirmation delivery failures after saving the pending user")
    void execute_confirmationDeliveryFails_keepsPersistenceFlowVisible() {
        var command = new RegisterCommand("Lucas", "lucas", "lucas@example.com", "RawPassword123!");
        when(passwordEncoder.hash(command.password())).thenReturn("hashed-password");
        when(codeConfirmation.generateCode()).thenReturn("123456");
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailSender).send("123456", "lucas@example.com");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("SMTP unavailable");

        verify(users).savePending(org.mockito.ArgumentMatchers.any(PendingUser.class), org.mockito.ArgumentMatchers.eq("123456"));
    }
}
