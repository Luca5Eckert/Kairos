package com.kairos.user.infrastructure.persistence.entity;

import com.kairos.module.user.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserEntity")
class UserEntityTest {

    @Test
    @DisplayName("uses UUID generation because PostgreSQL identity columns are numeric")
    void usesUuidGenerationForId() throws NoSuchFieldException {
        GeneratedValue generatedValue = UserEntity.class
                .getDeclaredField("id")
                .getAnnotation(GeneratedValue.class);

        assertThat(generatedValue.strategy()).isEqualTo(GenerationType.UUID);
    }
}
