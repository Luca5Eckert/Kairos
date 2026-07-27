package com.kairos.module.auth.application.use_case;

import com.kairos.module.auth.application.command.LoginCommand;
import com.kairos.module.auth.application.use_case.LoginUseCase;
import com.kairos.module.auth.domain.model.AuthenticatedSession;
import com.kairos.module.auth.domain.model.AuthenticatedUser;
import com.kairos.module.auth.domain.port.AuthenticatorPort;
import com.kairos.module.auth.domain.port.SessionIssuerPort;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock private AuthenticatorPort authenticator;
    @Mock private SessionIssuerPort sessionIssuer;

    @InjectMocks
    private LoginUseCase useCase;

    @Test
    @DisplayName("execute - authenticates credentials and issues session")
    void execute_validCredentials_issuesSession() {
        var command = new LoginCommand("lucas@example.com", "RawPassword123!");
        var authenticatedUser = new AuthenticatedUser(UUID.randomUUID(), "lucas@example.com", List.of(Role.FREE));
        var session = new AuthenticatedSession("access-token", List.of(Role.FREE));

        when(authenticator.authenticate(command.identifier(), command.password())).thenReturn(authenticatedUser);
        when(sessionIssuer.issueFor(authenticatedUser)).thenReturn(session);

        AuthenticatedSession result = useCase.execute(command);

        assertThat(result).isEqualTo(session);

        var inOrder = inOrder(authenticator, sessionIssuer);
        inOrder.verify(authenticator).authenticate("lucas@example.com", "RawPassword123!");
        inOrder.verify(sessionIssuer).issueFor(authenticatedUser);
    }

    @Test
    @DisplayName("execute - does not issue a session when authentication fails")
    void execute_invalidCredentials_doesNotIssueSession() {
        var command = new LoginCommand("lucas@example.com", "wrong-password");
        doThrow(new IllegalStateException("Invalid credentials"))
                .when(authenticator).authenticate(command.identifier(), command.password());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid credentials");

        verifyNoInteractions(sessionIssuer);
    }
}
