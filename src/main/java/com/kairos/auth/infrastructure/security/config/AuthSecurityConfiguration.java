package com.kairos.auth.infrastructure.security.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthSecurityConfiguration {

    @Bean
    public PasswordEncoder springPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(AuthProperties properties) {
        SecretKey key = new SecretKeySpec(
                properties.session().secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        return NimbusJwtEncoder.withSecretKey(key).build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
