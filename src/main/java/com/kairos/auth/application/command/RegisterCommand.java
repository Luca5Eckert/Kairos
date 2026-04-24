package com.kairos.auth.application.command;

public record RegisterCommand(
        String name,
        String username,
        String email,
        String password
) {
}
