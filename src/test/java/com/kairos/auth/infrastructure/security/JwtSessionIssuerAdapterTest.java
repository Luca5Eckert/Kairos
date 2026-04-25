package com.kairos.auth.infrastructure.security;

import com.kairos.auth.domain.model.AuthenticatedUser;
import com.kairos.auth.infrastructure.security.adapter.JwtSessionIssuerAdapter;
import com.kairos.auth.infrastructure.security.config.AuthProperties;
import com.kairos.user.domain.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSessionIssuerAdapterTest {

    @Test
    @DisplayName("issueFor - emits signed JWT access token preserving authenticated roles")
    void issueFor_authenticatedUser_emitsJwtSession() {
        String secret = "test-secret-with-at-least-32-bytes";
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder jwtEncoder = NimbusJwtEncoder.withSecretKey(key).build();

        var properties = new AuthProperties(
                new AuthProperties.Session(secret, "https://auth.kairos.test", "kairos-api", Duration.ofHours(1))
        );
        var clock = Clock.fixed(Instant.parse("2030-04-24T12:00:00Z"), ZoneOffset.UTC);
        var adapter = new JwtSessionIssuerAdapter(jwtEncoder, properties, clock);

        var user = new AuthenticatedUser(1L, "lucas@example.com", List.of(Role.FREE));

        var session = adapter.issueFor(user);

        assertThat(session.accessToken()).contains(".");
        assertThat(session.roles()).containsExactly(Role.FREE);

        var decoder = NimbusJwtDecoder.withSecretKey(key).build();
        var jwt = decoder.decode(session.accessToken());

        assertThat(jwt.getIssuer().toString()).isEqualTo("https://auth.kairos.test");
        assertThat(jwt.getAudience()).containsExactly("kairos-api");
        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("lucas@example.com");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("FREE");
        assertThat(jwt.getClaimAsString("scope")).isEqualTo("role:free");
    }
}
