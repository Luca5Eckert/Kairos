package com.kairos.auth.domain.model;

import com.kairos.user.domain.model.Role;

import java.util.List;

public record AuthenticatedSession(
        String accessToken,
        List<Role> roles
) {
}
