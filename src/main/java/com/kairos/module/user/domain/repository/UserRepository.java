package com.kairos.module.user.domain.repository;

import com.kairos.module.user.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID userId);

    User save(User user);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID userId);
}
