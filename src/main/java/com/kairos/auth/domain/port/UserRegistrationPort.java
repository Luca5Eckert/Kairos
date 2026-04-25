package com.kairos.auth.domain.port;

import com.kairos.auth.domain.model.AuthenticatedUser;
import com.kairos.auth.domain.model.PendingUser;

public interface UserRegistrationPort {
    void ensureEmailIsAvailable(String email);

    void ensureUsernameIsAvailable(String username);

    void savePending(PendingUser pendingUser, String code);

    AuthenticatedUser confirmEmail(String email, String code);
}
