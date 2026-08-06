package com.kairos.module.user.infrastructure.persistence.repository;

import com.kairos.module.user.domain.model.Role;
import com.kairos.module.user.domain.model.User;
import com.kairos.module.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.module.user.infrastructure.persistence.mapper.UserEntityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserEntityRepositoryTest {

    @Mock private UserEntityJpaRepository jpaRepository;

    private final UserEntityMapper mapper = new UserEntityMapper();

    @Test
    void save_persistsProfileAndUpdatedPasswordThroughJpaRepository() {
        UserEntityRepository repository = new UserEntityRepository(jpaRepository, mapper);
        User user = new User.Builder()
                .id(UUID.randomUUID())
                .name("Lucas")
                .username("lucas")
                .email("lucas@example.com")
                .hashPassword("new-hash")
                .role(Role.FREE)
                .emailConfirmed(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(jpaRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User saved = repository.save(user);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getHashPassword()).isEqualTo("new-hash");
        assertThat(saved.getHashPassword()).isEqualTo("new-hash");
        assertThat(saved.getCreatedAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    void findById_mapsStoredEntityToDomain() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = UserEntity.builder()
                .id(userId)
                .name("Lucas")
                .username("lucas")
                .email("lucas@example.com")
                .hashPassword("hash")
                .role(Role.FREE)
                .emailConfirmed(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(jpaRepository.findById(userId)).thenReturn(Optional.of(entity));

        User result = new UserEntityRepository(jpaRepository, mapper).findById(userId).orElseThrow();

        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getCreatedAt()).isEqualTo(entity.getCreatedAt());
        assertThat(result.getHashPassword()).isEqualTo("hash");
    }
}
