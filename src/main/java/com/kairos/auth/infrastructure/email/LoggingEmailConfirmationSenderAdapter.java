package com.kairos.auth.infrastructure.email;

import com.kairos.auth.domain.port.EmailConfirmationSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEmailConfirmationSenderAdapter implements EmailConfirmationSenderPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingEmailConfirmationSenderAdapter.class);

    @Override
    public void send(String code, String email) {
        LOGGER.info("Email confirmation requested | email={} code={}", email, code);
    }
}
