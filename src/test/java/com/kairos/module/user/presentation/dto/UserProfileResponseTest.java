package com.kairos.module.user.presentation.dto;

import com.kairos.module.user.domain.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileResponseTest {

    @Test
    void mapsMissingRoleToAnEmptyPublicRoleList() {
        User user = new User.Builder()
                .id(UUID.randomUUID())
                .name("Lucas")
                .username("lucas")
                .email("lucas@example.com")
                .build();

        assertThat(UserProfileResponse.from(user).roles()).isEmpty();
    }
}
