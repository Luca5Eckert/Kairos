package com.kairos.auth.domain.model;


public record PendingUser(
        String name,
        String username,
        String email,
        String passwordHash
) {
    public static PendingUser create(String name, String username, String email, String passwordHash) {
        return new PendingUser(name, username, email, passwordHash);
    }
}
