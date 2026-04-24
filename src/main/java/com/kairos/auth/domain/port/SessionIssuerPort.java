package com.kairos.auth.domain.port;

import com.kairos.auth.domain.model.AuthenticatedSession;
import com.kairos.auth.domain.model.AuthenticatedUser;

public interface SessionIssuerPort {

    AuthenticatedSession issueFor(AuthenticatedUser authenticatedUser);

}
