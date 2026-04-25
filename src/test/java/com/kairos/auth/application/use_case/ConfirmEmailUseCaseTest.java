package com.kairos.auth.application.use_case;

import com.kairos.auth.application.command.ConfirmEmailCommand;
import com.kairos.auth.domain.model.AuthenticatedSession;
import com.kairos.auth.domain.model.AuthenticatedUser;
import com.kairos.auth.domain.port.SessionIssuerPort;
import com.kairos.auth.domain.port.UserRegistrationPort;
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
class ConfirmEmailUseCaseTest {

    @Mock private UserRegistrationPort users;
    @Mock private SessionIssuerPort sessionIssuer;

    @InjectMocks
    private ConfirmEmailUseCase useCase;

    @Test
    @DisplayName("execute - confirms email and issues session without re-authenticating password")
    void execute_validCode_confirmsEmailAndIssuesSession() {
        var command = new ConfirmEmailCommand("123456", "lucas@example.com");
        var authenticatedUser = new AuthenticatedUser(1L, "lucas@example.com", List.of(Role.FREE));
        var session = new AuthenticatedSession("access-token", List.of(Role.FREE));

        when(users.confirmEmail(command.email(), command.code())).thenReturn(authenticatedUser);
        when(sessionIssuer.issueFor(authenticatedUser)).thenReturn(session);

        AuthenticatedSession result = useCase.execute(command);

        assertThat(result).isEqualTo(session);

        var inOrder = inOrder(users, sessionIssuer);
        inOrder.verify(users).confirmEmail("lucas@example.com", "123456");
        inOrder.verify(sessionIssuer).issueFor(authenticatedUser);
    }
}
