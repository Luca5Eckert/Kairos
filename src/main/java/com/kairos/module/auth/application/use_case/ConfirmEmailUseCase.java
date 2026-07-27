package com.kairos.module.auth.application.use_case;

import com.kairos.module.auth.application.command.ConfirmEmailCommand;
import com.kairos.module.auth.domain.model.AuthenticatedSession;
import com.kairos.module.auth.domain.model.AuthenticatedUser;
import com.kairos.module.auth.domain.port.SessionIssuerPort;
import com.kairos.module.auth.domain.port.UserRegistrationPort;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ConfirmEmailUseCase {

    private final UserRegistrationPort users;
    private final SessionIssuerPort sessionIssuer;

    public ConfirmEmailUseCase(UserRegistrationPort users, SessionIssuerPort sessionIssuer) {
        this.users = users;
        this.sessionIssuer = sessionIssuer;
    }

    @Transactional
    public AuthenticatedSession execute(ConfirmEmailCommand command) {
        AuthenticatedUser authenticatedUser = users.confirmEmail(command.email(), command.code());

        return sessionIssuer.issueFor(authenticatedUser);
    }

}
