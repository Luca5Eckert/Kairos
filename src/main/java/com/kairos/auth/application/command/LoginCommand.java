package com.kairos.auth.application.command;

/**
 * Command object for user login.
 * @param identifier can be either username or email.
 * @param password the user's password.
 */
public record LoginCommand(
        String identifier,
        String password
) {
    public static LoginCommand of(String identifier, String password) {
        return new LoginCommand(identifier, password);
    }
}
