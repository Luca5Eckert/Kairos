package com.kairos.module.auth.domain.port;

import com.kairos.module.auth.domain.model.AuthenticatedSession;
import com.kairos.module.auth.domain.model.AuthenticatedUser;

public interface SessionIssuerPort {

    AuthenticatedSession issueFor(AuthenticatedUser authenticatedUser);

}
