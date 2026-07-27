package com.kairos.module.auth.presentation.dto.register;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailRequest(
        @NotBlank String code,
        @NotBlank String email
) {
}
