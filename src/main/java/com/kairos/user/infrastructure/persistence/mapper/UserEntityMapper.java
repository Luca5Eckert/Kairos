package com.kairos.user.infrastructure.persistence.mapper;

import com.kairos.user.domain.model.User;
import com.kairos.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserEntityMapper {

    public UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .hashPassword(user.getHashPassword())
                .role(user.getRole())
                .emailConfirmed(user.isEmailConfirmed())
                .confirmationCodeHash(user.getConfirmationCodeHash())
                .build();
    }

    public User toDomain(UserEntity userEntity) {
        return new User.Builder()
                .id(userEntity.getId())
                .name(userEntity.getName())
                .username(userEntity.getUsername())
                .email(userEntity.getEmail())
                .hashPassword(userEntity.getHashPassword())
                .role(userEntity.getRole())
                .emailConfirmed(userEntity.isEmailConfirmed())
                .confirmationCodeHash(userEntity.getConfirmationCodeHash())
                .build();
    }

}
