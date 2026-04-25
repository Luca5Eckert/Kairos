package com.kairos.auth.application.use_case;

import com.kairos.auth.application.command.RegisterCommand;
import com.kairos.auth.domain.model.PendingUser;
import com.kairos.auth.domain.policy.PasswordPolicy;
import com.kairos.auth.domain.port.CodeConfirmationPort;
import com.kairos.auth.domain.port.EmailConfirmationSenderPort;
import com.kairos.auth.domain.port.PasswordEncoderPort;
import com.kairos.auth.domain.port.UserRegistrationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
