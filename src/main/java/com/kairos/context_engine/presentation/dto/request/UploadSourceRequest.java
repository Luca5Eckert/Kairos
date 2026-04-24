package com.kairos.context_engine.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UploadSourceRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotNull UUID authorId
) {
}
