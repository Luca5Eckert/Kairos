package com.kairos.auth.presentation.dto.login;

import com.kairos.user.domain.model.Role;

import java.util.List;

public record LoginResponse(
        String accessToken,
        List<String> roles
) {
    public static LoginResponse create(String accessToken, List<Role> roles) {
        var roleNames = roles.stream()
                .map(Role::name)
                .toList();

        return new LoginResponse(accessToken, roleNames);
    }
}
