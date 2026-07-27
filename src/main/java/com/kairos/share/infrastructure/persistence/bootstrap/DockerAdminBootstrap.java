package com.kairos.share.infrastructure.persistence.bootstrap;

import com.kairos.module.user.domain.model.Role;
import com.kairos.module.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.module.user.infrastructure.persistence.repository.UserEntityJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("docker")
public class DockerAdminBootstrap implements ApplicationRunner {

    private final UserEntityJpaRepository users;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String name;
    private final String username;
    private final String email;
    private final String password;

    public DockerAdminBootstrap(
            UserEntityJpaRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${kairos.admin.bootstrap.enabled:true}") boolean enabled,
            @Value("${kairos.admin.name:Kairos Admin}") String name,
            @Value("${kairos.admin.username:admin}") String username,
            @Value("${kairos.admin.email:admin@kairos.local}") String email,
            @Value("${kairos.admin.password:Admin123!}") String password
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.name = name;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Docker admin bootstrap is disabled.");
            return;
        }

        if (users.existsByEmailIgnoreCase(email) || users.existsByUsernameIgnoreCase(username)) {
            log.info("Docker admin bootstrap skipped because user '{}' or email '{}' already exists.", username, email);
            return;
        }

        UserEntity admin = UserEntity.builder()
                .name(name)
                .username(username)
                .email(email)
                .hashPassword(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .emailConfirmed(true)
                .confirmationCodeHash(null)
                .build();

        users.save(admin);
        log.info("Docker admin user '{}' was created with email '{}'.", username, email);
    }
}
