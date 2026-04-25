package com.kairos.auth.application.use_case;

import com.kairos.auth.application.command.LoginCommand;
import com.kairos.auth.domain.model.AuthenticatedSession;
import com.kairos.auth.domain.model.AuthenticatedUser;
import com.kairos.auth.domain.port.AuthenticatorPort;
import com.kairos.auth.domain.port.SessionIssuerPort;
import com.kairos.user.domain.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
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
        var authenticatedUser = new AuthenticatedUser(1L, "lucas@example.com", List.of(Role.FREE));
        var session = new AuthenticatedSession("access-token", List.of(Role.FREE));

        when(authenticator.authenticate(command.identifier(), command.password())).thenReturn(authenticatedUser);
        when(sessionIssuer.issueFor(authenticatedUser)).thenReturn(session);

        AuthenticatedSession result = useCase.execute(command);

        assertThat(result).isEqualTo(session);

        var inOrder = inOrder(authenticator, sessionIssuer);
        inOrder.verify(authenticator).authenticate("lucas@example.com", "RawPassword123!");
        inOrder.verify(sessionIssuer).issueFor(authenticatedUser);
    }
}
