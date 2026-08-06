package com.kairos.module.user.application.command;

public record UpdateProfileCommand(
        String name,
        String username
) {
}
