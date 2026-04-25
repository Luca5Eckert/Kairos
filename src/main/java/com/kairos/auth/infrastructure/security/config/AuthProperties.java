package com.kairos.auth.infrastructure.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        Session session
) {
    public AuthProperties {
        if (session == null) {
            session = new Session("dev-only-change-me-dev-only-change-me", "https://auth.kairos.local", "kairos-api", Duration.ofHours(2));
        }
    }

    public record Session(
            String secret,
            String issuer,
            String audience,
            Duration accessTokenTtl
    ) {
        public Session {
            if (secret == null || secret.isBlank()) {
                secret = "dev-only-change-me-dev-only-change-me";
            }
            if (issuer == null || issuer.isBlank()) {
                issuer = "https://auth.kairos.local";
            }
            if (audience == null || audience.isBlank()) {
                audience = "kairos-api";
            }
            if (accessTokenTtl == null) {
                accessTokenTtl = Duration.ofHours(2);
            }
        }
    }
}
