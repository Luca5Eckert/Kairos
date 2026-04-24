package com.kairos.auth.domain.port;

import com.kairos.auth.domain.model.AuthenticatedUser;

public interface AuthenticatorPort {

    AuthenticatedUser authenticate(String identifier, String rawPassword);

}
