package com.kairos.application.command;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UploadSourceCommand(
        String title,
        String content,
        UUID authorId
) {
    public static UploadSourceCommand of(String title, String content, UUID authorId) {
        return new UploadSourceCommand(title, content, authorId);
    }
}
