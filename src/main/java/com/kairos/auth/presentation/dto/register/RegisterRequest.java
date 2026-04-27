package com.kairos.auth.presentation.dto.register;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank String username,
        @NotBlank String email,
        @NotBlank String password
) {
}
