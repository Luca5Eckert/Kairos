package com.kairos.auth.application.use_case;

import com.kairos.auth.application.command.ConfirmEmailCommand;
import com.kairos.auth.domain.exception.AuthenticationDomainException;
import com.kairos.auth.domain.model.AuthenticatedSession;
import com.kairos.auth.domain.model.AuthenticatedUser;
import com.kairos.auth.domain.model.PendingUser;
import com.kairos.auth.domain.port.AuthenticatorPort;
import com.kairos.auth.domain.port.CodeConfirmationPort;
import com.kairos.auth.domain.port.SessionIssuerPort;
import com.kairos.auth.domain.port.UserRegistrationPort;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ConfirmEmailUseCase {

    private final UserRegistrationPort users;
    private final CodeConfirmationPort codeConfirmation;
    private final AuthenticatorPort authenticator;
    private final SessionIssuerPort sessionIssuer;

    public ConfirmEmailUseCase(UserRegistrationPort users, CodeConfirmationPort codeConfirmation, AuthenticatorPort authenticator, SessionIssuerPort sessionIssuer) {
        this.users = users;
        this.codeConfirmation = codeConfirmation;
        this.authenticator = authenticator;
        this.sessionIssuer = sessionIssuer;
    }

    @Transactional
    public AuthenticatedSession execute(ConfirmEmailCommand command) {
        if(!codeConfirmation.validateCode(command.code(), command.email())){
            throw new AuthenticationDomainException("Confirmation code is invalid");
        }

        PendingUser pendingUser = users.getPendingUserByEmail(command.email())
                .orElseThrow(() -> new AuthenticationDomainException("No pending user found for the provided email"));

        // PROBLEM - THE PASSWORD WILL BE A HASH, BUT THE AUTHENTICATOR EXPECTS THE RAW PASSWORD TO COMPARE WITH THE HASH.
        // THIS IS A PROBLEM BECAUSE WE DON'T HAVE THE RAW PASSWORD STORED ANYWHERE, AND WE CAN'T REVERSE THE HASH.
        AuthenticatedUser authenticatedUser = authenticator.authenticate(pendingUser.email(), pendingUser.password());

        return sessionIssuer.issueFor(authenticatedUser);
    }

}
