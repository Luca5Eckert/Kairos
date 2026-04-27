package com.kairos.auth.application.command;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailCommand(
        String code,
        String email
) {
    public static ConfirmEmailCommand create(String code,String email) {
        return new ConfirmEmailCommand(code,email);
    }
}
