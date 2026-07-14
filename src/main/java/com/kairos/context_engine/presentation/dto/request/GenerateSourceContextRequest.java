package com.kairos.context_engine.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GenerateSourceContextRequest(
        @NotBlank String termQuery
) {
}
