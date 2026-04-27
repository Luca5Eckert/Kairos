package com.kairos.auth.application.command;

public record ConfirmEmailCommand(
        String code,
        String email
) {
    public static ConfirmEmailCommand of(String code, String email) {
        return new ConfirmEmailCommand(code,email);
    }
}
