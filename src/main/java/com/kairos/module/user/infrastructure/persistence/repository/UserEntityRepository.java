package com.kairos.module.user.infrastructure.persistence.repository;

import com.kairos.module.user.domain.model.User;
import com.kairos.module.user.domain.repository.UserRepository;
import com.kairos.module.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.module.user.infrastructure.persistence.mapper.UserEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserEntityRepository implements UserRepository {

    private final UserEntityJpaRepository jpaRepository;

    private final UserEntityMapper mapper;

    public UserEntityRepository(UserEntityJpaRepository jpaRepository, UserEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);

        jpaRepository.save(entity);

        return mapper.toDomain(entity);
    }

    public Optional<User> findById(UUID id) {
        var entity = jpaRepository.findById(id);
        return entity.map(mapper::toDomain);
    }

}
