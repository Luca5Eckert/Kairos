package com.kairos.module.auth.domain.model;

import com.kairos.module.user.domain.model.Role;

import java.util.List;

public record AuthenticatedSession(
        String accessToken,
        List<Role> roles
) {
}
