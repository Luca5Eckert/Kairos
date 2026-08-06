package com.kairos.module.user.application.use_case;

import com.kairos.module.auth.domain.exception.AuthenticationDomainException;
import com.kairos.module.auth.domain.policy.PasswordPolicy;
import com.kairos.module.auth.domain.port.PasswordEncoderPort;
import com.kairos.module.user.application.command.ChangePasswordCommand;
import com.kairos.module.user.domain.model.User;
import com.kairos.module.user.domain.repository.UserRepository;
import com.kairos.share.security.context.RequestContextProvider;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChangeMyPasswordUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangeMyPasswordUseCase.class);

    private final UserRepository users;
    private final RequestContextProvider requestContextProvider;
    private final PasswordEncoderPort passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public ChangeMyPasswordUseCase(UserRepository users, RequestContextProvider requestContextProvider,
                                   PasswordEncoderPort passwordEncoder, PasswordPolicy passwordPolicy) {
        this.users = users;
        this.requestContextProvider = requestContextProvider;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    @Transactional
    public void execute(ChangePasswordCommand command) {
        var userId = requestContextProvider.getRequestContext().userId();
        User user = users.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user was not found"));

        if (!passwordEncoder.matches(command.currentPassword(), user.getHashPassword())) {
            throw new AuthenticationDomainException("Current password is invalid");
        }
        if (!command.newPassword().equals(command.newPasswordConfirmation())) {
            throw new IllegalArgumentException("New password confirmation does not match");
        }

        passwordPolicy.validate(command.newPassword());
        user.changePassword(passwordEncoder.hash(command.newPassword()));

        users.save(user);

        log.info("security_event=password_changed user_id={}", userId);
    }
}
