package com.kairos.module.user.domain.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UsernameNormalizerTest {

    @Test
    void normalizesWhitespaceAndCase() {
        assertThat(UsernameNormalizer.normalize("  Lucas.Dev  ")).isEqualTo("lucas.dev");
    }

    @Test
    void rejectsNullUsername() {
        assertThatIllegalArgumentException().isThrownBy(() -> UsernameNormalizer.normalize(null));
    }

    @Test
    void rejectsBlankUsername() {
        assertThatIllegalArgumentException().isThrownBy(() -> UsernameNormalizer.normalize("   "));
    }
}
