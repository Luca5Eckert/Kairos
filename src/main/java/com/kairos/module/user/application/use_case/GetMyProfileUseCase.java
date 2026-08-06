package com.kairos.module.user.application.use_case;

import com.kairos.module.user.domain.model.User;
import com.kairos.module.user.domain.repository.UserRepository;
import com.kairos.share.security.context.RequestContextProvider;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetMyProfileUseCase {

    private final UserRepository users;
    private final RequestContextProvider requestContextProvider;

    public GetMyProfileUseCase(UserRepository users, RequestContextProvider requestContextProvider) {
        this.users = users;
        this.requestContextProvider = requestContextProvider;
    }

    public User execute() {
        var userId = requestContextProvider.getRequestContext().userId();
        return users.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user was not found"));
    }
}
