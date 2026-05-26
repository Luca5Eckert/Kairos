package com.kairos.auth.domain.model;

import com.kairos.user.domain.model.Role;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        List<Role> roles
) {
}
