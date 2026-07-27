package com.kairos.module.auth.infrastructure.email;

import com.kairos.module.auth.domain.port.EmailConfirmationSenderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailConfirmationSenderAdapter implements EmailConfirmationSenderPort {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailConfirmationSenderAdapter(
            JavaMailSender mailSender,
            @Value("${kairos.mail.from}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String code, String email) {
        try {
            mailSender.send(createMessage(code, email));
        } catch (MailException e) {
            throw new EmailConfirmationDeliveryException("Could not send email confirmation code", e);
        }
    }

    private SimpleMailMessage createMessage(String code, String email) {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Confirme seu email no Kairos");
        message.setText("""
                Ola!

                Seu codigo de confirmacao no Kairos e: %s

                Se voce nao criou uma conta, ignore este email.
                """.formatted(code));
        return message;
    }
}
