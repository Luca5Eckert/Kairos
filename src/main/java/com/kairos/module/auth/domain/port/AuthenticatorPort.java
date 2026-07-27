package com.kairos.module.auth.domain.port;

import com.kairos.module.auth.domain.model.AuthenticatedUser;

public interface AuthenticatorPort {

    AuthenticatedUser authenticate(String identifier, String rawPassword);

}
