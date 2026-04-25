package com.kairos.auth.infrastructure.security.adapter;

import com.kairos.auth.domain.model.AuthenticatedSession;
import com.kairos.auth.domain.model.AuthenticatedUser;
import com.kairos.auth.domain.port.SessionIssuerPort;
import com.kairos.auth.infrastructure.security.config.AuthProperties;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

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
                .subject(String.valueOf(authenticatedUser.id()))
                .claim("email", authenticatedUser.email())
                .claim("roles", authenticatedUser.roles())
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return new AuthenticatedSession(accessToken, authenticatedUser.roles());
    }
}
