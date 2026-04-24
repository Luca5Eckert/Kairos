package com.kairos.auth.application.use_case;

import com.kairos.auth.application.command.LoginCommand;
import com.kairos.auth.domain.model.AuthenticatedSession;
import com.kairos.auth.domain.model.AuthenticatedUser;
import com.kairos.auth.domain.port.AuthenticatorPort;
import com.kairos.auth.domain.port.SessionIssuerPort;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {

    private final AuthenticatorPort authenticator;
    private final SessionIssuerPort sessionIssuer;

    public LoginUseCase(AuthenticatorPort authenticator, SessionIssuerPort sessionIssuer) {
        this.authenticator = authenticator;
        this.sessionIssuer = sessionIssuer;
    }

    public AuthenticatedSession execute(LoginCommand command) {
        AuthenticatedUser authenticatedUser = authenticator.authenticate(command.identifier(), command.password());

        return sessionIssuer.issueFor(authenticatedUser);
    }

}
