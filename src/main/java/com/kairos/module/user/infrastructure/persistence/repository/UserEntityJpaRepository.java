package com.kairos.module.user.infrastructure.persistence.repository;

import com.kairos.module.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserEntityJpaRepository extends JpaRepository<UserEntity, UUID> {
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Optional<UserEntity> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);
}
