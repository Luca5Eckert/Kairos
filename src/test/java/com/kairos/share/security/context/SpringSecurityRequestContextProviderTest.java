package com.kairos.share.security.context;

import com.kairos.module.user.domain.model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringSecurityRequestContextProviderTest {

    private final SpringSecurityRequestContextProvider provider = new SpringSecurityRequestContextProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getRequestContext - maps authenticated JWT into request context")
    void getRequestContext_authenticatedJwt_returnsRequestContext() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwt(userId.toString())
                .claim("email", "lucas@example.com")
                .claim("roles", List.of("FREE", "ADMIN"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_FREE"), new SimpleGrantedAuthority("ROLE_ADMIN")),
                jwt.getSubject()
        ));

        RequestContext result = provider.getRequestContext();

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.email()).isEqualTo("lucas@example.com");
        assertThat(result.roles()).containsExactly(Role.FREE, Role.ADMIN);
    }

    @Test
    @DisplayName("getRequestContext - fails when there is no authenticated user")
    void getRequestContext_withoutAuthentication_throwsAuthenticationException() {
        assertThatThrownBy(provider::getRequestContext)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("Authenticated request context is required");
    }

    @Test
    @DisplayName("getRequestContext - fails when authenticated user id is malformed")
    void getRequestContext_malformedUserId_throwsBadCredentialsException() {
        Jwt jwt = jwt("not-a-uuid")
                .claim("email", "lucas@example.com")
                .claim("roles", List.of("FREE"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_FREE")),
                jwt.getSubject()
        ));

        assertThatThrownBy(provider::getRequestContext)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Authenticated user id must be a valid UUID");
    }

    private Jwt.Builder jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuer("https://auth.kairos.test")
                .subject(subject)
                .audience(List.of("kairos-api"))
                .issuedAt(Instant.parse("2026-04-24T12:00:00Z"))
                .expiresAt(Instant.parse("2026-04-24T13:00:00Z"));
    }
}
