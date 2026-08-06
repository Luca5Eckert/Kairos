package com.kairos.module.user.domain.policy;

import java.util.Locale;

public final class UsernameNormalizer {

    private UsernameNormalizer() {
    }

    public static String normalize(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
