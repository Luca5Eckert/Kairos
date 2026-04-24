package com.kairos.auth.application.use_case;

import com.kairos.auth.application.command.RegisterCommand;
import com.kairos.auth.domain.model.PendingUser;
import com.kairos.auth.domain.policy.PasswordPolicy;
import com.kairos.auth.domain.port.CodeConfirmationPort;
import com.kairos.auth.domain.port.EmailConfirmationSenderPort;
import com.kairos.auth.domain.port.PasswordEncoderPort;
import com.kairos.auth.domain.port.UserRegistrationPort;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RegisterUseCase {
    
    private final PasswordPolicy passwordPolicy;

    private final UserRegistrationPort users;
    private final PasswordEncoderPort passwordEncoder;
    private final CodeConfirmationPort codeConfirmation;
    private final EmailConfirmationSenderPort emailSender;

    public RegisterUseCase(UserRegistrationPort users, PasswordPolicy passwordPolicy, PasswordEncoderPort passwordEncoder, CodeConfirmationPort codeConfirmation, EmailConfirmationSenderPort emailSender) {
        this.users = users;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.codeConfirmation = codeConfirmation;
        this.emailSender = emailSender;
    }

    @Transactional
    public void execute(RegisterCommand command) {
        users.ensureEmailIsAvailable(command.email());
        users.ensureUsernameIsAvailable(command.username());

        passwordPolicy.validate(command.password());

        String passwordHash = passwordEncoder.hash(command.password());
        
        var pendingUser = PendingUser.create(
                command.name(),
                command.username(),
                command.email(),
                passwordHash
        );

        String code = codeConfirmation.generateCode();

        users.savePending(pendingUser, code);
        
        emailSender.send(code, pendingUser.email());
    }
    
}
