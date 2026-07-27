package com.kairos.module.context_engine.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UploadSourceRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
