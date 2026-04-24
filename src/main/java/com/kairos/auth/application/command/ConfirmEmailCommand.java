package com.kairos.auth.application.command;

public record ConfirmEmailCommand(
        String code,
        String email
) {
}
