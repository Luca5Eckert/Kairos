package com.kairos.auth.application.command;

public record RegisterCommand(
        String name,
        String username,
        String email,
        String password
) {
    public static RegisterCommand create(String name, String username, String email, String password) {
        return new RegisterCommand(
                name,
                username,
                email,
                password
        );
    }
}
