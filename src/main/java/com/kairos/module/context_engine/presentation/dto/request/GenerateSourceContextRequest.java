package com.kairos.module.context_engine.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GenerateSourceContextRequest(
        @NotBlank String termQuery
) {
}
