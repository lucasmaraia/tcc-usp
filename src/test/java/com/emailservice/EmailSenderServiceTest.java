package com.emailservice;

import com.emailservice.domain.exception.EmailSendException;
import com.emailservice.infrastructure.service.EmailSenderService;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailSenderServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MimeMessage stubMimeMessage() {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        return message;
    }

    @Test
    void sendEmail_Success() throws Exception {
        MimeMessage message = stubMimeMessage();
        var service = new EmailSenderService(mailSender, "sender@example.com");

        service.sendEmail("dest@example.com", "Hello", "<html><body>Hi</body></html>");

        verify(mailSender).send(message);
        assertEquals("sender@example.com", message.getFrom()[0].toString());
        assertEquals("dest@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("Hello", message.getSubject());
    }

    @Test
    void sendEmail_EmptyFromAddress_ThrowsEmailSendException() {
        stubMimeMessage();
        var service = new EmailSenderService(mailSender, "");

        assertThrows(EmailSendException.class, () ->
                service.sendEmail("dest@example.com", "Hello", "<html></html>"));
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void sendEmail_InvalidRecipient_ThrowsEmailSendException() {
        stubMimeMessage();
        var service = new EmailSenderService(mailSender, "sender@example.com");

        assertThrows(EmailSendException.class, () ->
                service.sendEmail("not an address", "Hello", "<html></html>"));
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }
}
