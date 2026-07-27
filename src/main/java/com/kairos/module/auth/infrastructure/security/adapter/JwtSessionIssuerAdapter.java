package com.kairos.module.auth.infrastructure.security.adapter;

import com.kairos.module.auth.domain.model.AuthenticatedSession;
import com.kairos.module.auth.domain.model.AuthenticatedUser;
import com.kairos.module.auth.domain.port.SessionIssuerPort;
import com.kairos.share.security.config.AuthProperties;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;

@Component
public class JwtSessionIssuerAdapter implements SessionIssuerPort {

    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;
    private final Clock clock;

    public JwtSessionIssuerAdapter(JwtEncoder jwtEncoder, AuthProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public AuthenticatedSession issueFor(AuthenticatedUser authenticatedUser) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.session().accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.session().issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .audience(java.util.List.of(properties.session().audience()))
                .subject(String.valueOf(authenticatedUser.id()))
                .claim("email", authenticatedUser.email())
                .claim("roles", authenticatedUser.roles().stream().map(Enum::name).toList())
                .claim("scope", authenticatedUser.roles().stream()
                        .map(role -> "role:" + role.name().toLowerCase())
                        .collect(Collectors.joining(" ")))
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return new AuthenticatedSession(accessToken, authenticatedUser.roles());
    }
}
