package com.kairos.auth.infrastructure.confirmation;

import com.kairos.auth.domain.port.CodeConfirmationPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureCodeConfirmationAdapter implements CodeConfirmationPort {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_BOUND = 1_000_000;

    @Override
    public String generateCode() {
        return "%06d".formatted(RANDOM.nextInt(CODE_BOUND));
    }
}
