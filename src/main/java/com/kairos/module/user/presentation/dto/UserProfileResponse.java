package com.kairos.module.user.presentation.dto;

import com.kairos.module.user.domain.model.Role;
import com.kairos.module.user.domain.model.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String username,
        String email,
        boolean emailConfirmed,
        List<Role> roles,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.isEmailConfirmed(),
                user.getRole() == null ? List.of() : List.of(user.getRole()),
                user.getCreatedAt()
        );
    }
}
