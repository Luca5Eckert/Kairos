package com.kairos.module.auth.application.use_case;

import com.kairos.module.auth.application.command.ConfirmEmailCommand;
import com.kairos.module.auth.domain.model.AuthenticatedSession;
import com.kairos.module.auth.domain.model.AuthenticatedUser;
import com.kairos.module.auth.domain.port.SessionIssuerPort;
import com.kairos.module.auth.domain.port.UserRegistrationPort;
import com.kairos.module.user.domain.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmEmailUseCaseTest {

    @Mock private UserRegistrationPort users;
    @Mock private SessionIssuerPort sessionIssuer;

    @InjectMocks
    private ConfirmEmailUseCase useCase;

    @Test
    @DisplayName("execute - confirms email and issues session without re-authenticating password")
    void execute_validCode_confirmsEmailAndIssuesSession() {
        var command = new ConfirmEmailCommand("123456", "lucas@example.com");
        var authenticatedUser = new AuthenticatedUser(UUID.randomUUID(), "lucas@example.com", List.of(Role.FREE));
        var session = new AuthenticatedSession("access-token", List.of(Role.FREE));

        when(users.confirmEmail(command.email(), command.code())).thenReturn(authenticatedUser);
        when(sessionIssuer.issueFor(authenticatedUser)).thenReturn(session);

        AuthenticatedSession result = useCase.execute(command);

        assertThat(result).isEqualTo(session);

        var inOrder = inOrder(users, sessionIssuer);
        inOrder.verify(users).confirmEmail("lucas@example.com", "123456");
        inOrder.verify(sessionIssuer).issueFor(authenticatedUser);
    }

    @Test
    @DisplayName("execute - does not issue a session when confirmation code is invalid")
    void execute_invalidCode_doesNotIssueSession() {
        var command = new ConfirmEmailCommand("999999", "lucas@example.com");
        doThrow(new IllegalStateException("Confirmation code is invalid"))
                .when(users).confirmEmail(command.email(), command.code());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Confirmation code is invalid");

        verifyNoInteractions(sessionIssuer);
    }
}
