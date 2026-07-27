package com.kairos.share.security.context;

import com.kairos.module.user.domain.model.Role;

import java.util.List;
import java.util.UUID;

public record RequestContext(
        UUID userId,
        String email,
        List<Role> roles
) {
}
