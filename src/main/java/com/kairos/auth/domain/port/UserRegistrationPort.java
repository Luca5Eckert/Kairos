package com.kairos.auth.domain.port;

import com.kairos.auth.domain.model.PendingUser;

import java.util.Optional;

public interface UserRegistrationPort {
    void ensureEmailIsAvailable(String email);

    void ensureUsernameIsAvailable(String username);

    void savePending(PendingUser pendingUser, String code);

    Optional<PendingUser> getPendingUserByEmail(String email);
}
