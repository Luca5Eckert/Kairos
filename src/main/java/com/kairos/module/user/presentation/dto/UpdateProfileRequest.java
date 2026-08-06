package com.kairos.module.user.presentation.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 255) String name,
        @Size(max = 100) String username
) {
}
