package com.kairos.auth.infrastructure.security;

import com.kairos.auth.infrastructure.security.adapter.SpringPasswordEncoderAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SpringPasswordEncoderAdapterTest {

    private final SpringPasswordEncoderAdapter encoder =
            new SpringPasswordEncoderAdapter(new BCryptPasswordEncoder());

    @Test
    @DisplayName("hash - delegates to Spring Security and does not expose plain text")
    void hash_returnsEncodedPassword() {
        String hash = encoder.hash("RawPassword123!");

        assertThat(hash).startsWith("$2");
        assertThat(hash).doesNotContain("RawPassword123!");
    }

    @Test
    @DisplayName("matches - returns true only for the original password")
    void matches_validatesOriginalPassword() {
        String hash = encoder.hash("RawPassword123!");

        assertThat(encoder.matches("RawPassword123!", hash)).isTrue();
        assertThat(encoder.matches("wrong-password", hash)).isFalse();
    }
}
