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
}
