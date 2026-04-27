package com.kairos.auth.application.command;

public record RegisterCommand(
        String name,
        String username,
        String email,
        String password
) {
    public static RegisterCommand of(String name, String username, String email, String password) {
        return new RegisterCommand(
                name,
                username,
                email,
                password
        );
    }
}
