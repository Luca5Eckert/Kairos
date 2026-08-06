package com.kairos.module.user.application.command;

public record ChangePasswordCommand(
        String currentPassword,
        String newPassword,
        String newPasswordConfirmation
) {
}
