package com.kairos.user.infrastructure.persistence.mapper;

import com.kairos.module.user.domain.model.Role;
import com.kairos.module.user.domain.model.User;
import com.kairos.module.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.module.user.infrastructure.persistence.mapper.UserEntityMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityMapperTest {

    private final UserEntityMapper mapper = new UserEntityMapper();

    @Test
    void toEntity_preservesAllUserFields() {
        UUID userId = UUID.randomUUID();
        User user = new User.Builder()
                .id(userId)
                .name("Lucas")
                .username("lucas")
                .email("lucas@example.com")
                .hashPassword("hashed-password")
                .role(Role.ADMIN)
                .emailConfirmed(true)
                .confirmationCodeHash("confirmation-hash")
                .build();

        UserEntity entity = mapper.toEntity(user);

        assertThat(entity.getId()).isEqualTo(userId);
        assertThat(entity.getName()).isEqualTo("Lucas");
        assertThat(entity.getUsername()).isEqualTo("lucas");
        assertThat(entity.getEmail()).isEqualTo("lucas@example.com");
        assertThat(entity.getHashPassword()).isEqualTo("hashed-password");
        assertThat(entity.getRole()).isEqualTo(Role.ADMIN);
        assertThat(entity.isEmailConfirmed()).isTrue();
        assertThat(entity.getConfirmationCodeHash()).isEqualTo("confirmation-hash");
    }

    @Test
    void toDomain_preservesAllEntityFields() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = UserEntity.builder()
                .id(userId)
                .name("Lucas")
                .username("lucas")
                .email("lucas@example.com")
                .hashPassword("hashed-password")
                .role(Role.PREMIUM)
                .emailConfirmed(false)
                .confirmationCodeHash("confirmation-hash")
                .build();

        User user = mapper.toDomain(entity);

        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getName()).isEqualTo("Lucas");
        assertThat(user.getUsername()).isEqualTo("lucas");
        assertThat(user.getEmail()).isEqualTo("lucas@example.com");
        assertThat(user.getHashPassword()).isEqualTo("hashed-password");
        assertThat(user.getRole()).isEqualTo(Role.PREMIUM);
        assertThat(user.isEmailConfirmed()).isFalse();
        assertThat(user.getConfirmationCodeHash()).isEqualTo("confirmation-hash");
    }
}
