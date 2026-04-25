package com.kairos.auth.infrastructure.security;

import com.kairos.auth.infrastructure.security.config.AuthSecurityConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSecurityConfigurationTest {

    private final AuthSecurityConfiguration configuration = new AuthSecurityConfiguration();

    @Test
    @DisplayName("jwtAuthenticationConverter - maps roles and scopes to Spring authorities")
    void jwtAuthenticationConverter_mapsRolesAndScopesToAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuer("https://auth.kairos.test")
                .subject("1")
                .audience(List.of("kairos-api"))
                .issuedAt(Instant.parse("2026-04-24T12:00:00Z"))
                .expiresAt(Instant.parse("2026-04-24T13:00:00Z"))
                .claim("roles", List.of("ADMIN", "FREE"))
                .claim("scope", "source:write user:read")
                .build();

        var authentication = configuration.jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("1");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder(
                        "ROLE_ADMIN",
                        "ROLE_FREE",
                        "SCOPE_source:write",
                        "SCOPE_user:read"
                );
    }
}
