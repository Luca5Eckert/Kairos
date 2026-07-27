package com.kairos.module.auth.infrastructure.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpEmailConfirmationSenderAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    @DisplayName("send - sends confirmation code email through JavaMailSender")
    void send_validCode_sendsConfirmationEmail() {
        var adapter = new SmtpEmailConfirmationSenderAdapter(mailSender, "no-reply@kairos.local");

        adapter.send("123456", "lucas@example.com");

        var messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@kairos.local");
        assertThat(message.getTo()).containsExactly("lucas@example.com");
        assertThat(message.getSubject()).isEqualTo("Confirme seu email no Kairos");
        assertThat(message.getText()).contains("123456");
    }

    @Test
    @DisplayName("send - wraps mail sender failures as email confirmation delivery failures")
    void send_mailSenderFails_throwsEmailConfirmationDeliveryException() {
        var adapter = new SmtpEmailConfirmationSenderAdapter(mailSender, "no-reply@kairos.local");
        var failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertThatThrownBy(() -> adapter.send("123456", "lucas@example.com"))
                .isInstanceOf(EmailConfirmationDeliveryException.class)
                .hasMessage("Could not send email confirmation code")
                .hasCause(failure);
    }
}
