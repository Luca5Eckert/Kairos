package com.kairos.module.context_engine.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record GenerateSourceContextRequest(
        @NotBlank String termQuery,
        UUID questionId
) {
}
