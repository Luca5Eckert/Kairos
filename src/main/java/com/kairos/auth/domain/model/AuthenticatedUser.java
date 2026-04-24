package com.kairos.auth.domain.model;

import com.kairos.user.domain.model.Role;

import java.util.List;

public record AuthenticatedUser(
        Long id,
        String email,
        List<Role> roles
) {
}
