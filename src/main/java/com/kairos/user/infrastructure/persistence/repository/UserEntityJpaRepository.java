package com.kairos.user.infrastructure.persistence.repository;

import com.kairos.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEntityJpaRepository extends JpaRepository<UserEntity, Long> {
}
