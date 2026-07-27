package com.kairos.module.auth.domain.model;

import com.kairos.module.user.domain.model.Role;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        List<Role> roles
) {
}
