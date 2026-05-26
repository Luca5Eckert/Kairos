package com.kairos.share.security.context;

import com.kairos.user.domain.model.Role;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SpringSecurityRequestContextProvider implements RequestContextProvider {

    @Override
    public RequestContext getRequestContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated request context is required");
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated request context must contain a JWT principal");
        }

        return new RequestContext(
                parseUserId(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                parseRoles(jwt.getClaimAsStringList("roles"))
        );
    }

    private UUID parseUserId(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Authenticated user id must be a valid UUID", e);
        }
    }

    private List<Role> parseRoles(List<String> roles) {
        if (roles == null) {
            return List.of();
        }

        try {
            return roles.stream()
                    .filter(role -> role != null && !role.isBlank())
                    .map(role -> role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role)
                    .map(Role::valueOf)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Authenticated roles claim contains an unknown role", e);
        }
    }
}
