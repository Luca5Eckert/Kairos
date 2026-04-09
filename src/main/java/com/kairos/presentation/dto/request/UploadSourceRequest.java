package com.kairos.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UploadSourceRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotBlank UUID authorId
) {
}
