package com.kairos.module.user.application.use_case;

import com.kairos.module.auth.domain.exception.AuthenticationDomainException;
import com.kairos.module.user.application.command.UpdateProfileCommand;
import com.kairos.module.user.domain.model.User;
import com.kairos.module.user.domain.policy.UsernameNormalizer;
import com.kairos.module.user.domain.repository.UserRepository;
import com.kairos.share.security.context.RequestContextProvider;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UpdateMyProfileUseCase {

    private final UserRepository users;
    private final RequestContextProvider requestContextProvider;

    public UpdateMyProfileUseCase(UserRepository users, RequestContextProvider requestContextProvider) {
        this.users = users;
        this.requestContextProvider = requestContextProvider;
    }

    @Transactional
    public User execute(UpdateProfileCommand command) {
        if (command.name() == null && command.username() == null) {
            throw new IllegalArgumentException("At least one profile field is required");
        }

        var userId = requestContextProvider.getRequestContext().userId();
        User user = users.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user was not found"));

        String name = command.name();
        if (name != null) {
            name = name.trim();
            if (name.isBlank()) {
                throw new IllegalArgumentException("Name is required");
            }
        }

        String username = command.username() == null
                ? null
                : UsernameNormalizer.normalize(command.username());
        if (username != null && users.existsByUsernameIgnoreCaseAndIdNot(username, userId)) {
            throw new AuthenticationDomainException("Username is already in use");
        }

        user.updateProfile(name, username);

        return users.save(user);
    }
}
