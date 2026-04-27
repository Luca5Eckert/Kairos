package com.kairos.auth.presentation.dto.login;

import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequest is a record that represents the data required for a login request.
 * @param identifier The identifier can be either a username or an email address.
 * @param password The password associated with the identifier.
 */
public record LoginRequest(
        @NotBlank String identifier,
        @NotBlank String password
) {
}
