package com.kairos.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UploadSourceRequest(
        @NotBlank String text
) {
}
