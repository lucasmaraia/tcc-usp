package com.emailservice;

import com.emailservice.application.dto.EmailAttachmentDto;
import com.emailservice.domain.exception.EmailSendException;
import com.emailservice.infrastructure.service.SmtpEmailSender;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

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
        var service = new SmtpEmailSender(mailSender, "sender@example.com");

        service.sendEmail("dest@example.com", "Hello", "<html><body>Hi</body></html>");

        verify(mailSender).send(message);
        assertEquals("sender@example.com", message.getFrom()[0].toString());
        assertEquals("dest@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("Hello", message.getSubject());
    }

    @Test
    void sendEmail_WithAttachment_AddsAttachmentPart() throws Exception {
        MimeMessage message = stubMimeMessage();
        var service = new SmtpEmailSender(mailSender, "sender@example.com");
        String base64 = Base64.getEncoder().encodeToString("conteudo".getBytes(StandardCharsets.UTF_8));
        var attachment = new EmailAttachmentDto("relatorio.pdf", "application/pdf", base64);

        service.sendEmail("dest@example.com", "Hello", "<html><body>Hi</body></html>", List.of(attachment));

        verify(mailSender).send(message);
        MimeMultipart multipart = (MimeMultipart) message.getContent();
        BodyPart attachmentPart = null;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if ("relatorio.pdf".equals(part.getFileName())) {
                attachmentPart = part;
            }
        }
        assertNotNull(attachmentPart, "Attachment part not found in MIME message");
    }

    @Test
    void sendEmail_InvalidBase64Attachment_ThrowsEmailSendException() {
        stubMimeMessage();
        var service = new SmtpEmailSender(mailSender, "sender@example.com");
        var attachment = new EmailAttachmentDto("arquivo.txt", "text/plain", "not-valid-base64!!!");

        assertThrows(EmailSendException.class, () ->
                service.sendEmail("dest@example.com", "Hello", "<html></html>", List.of(attachment)));
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void sendEmail_EmptyFromAddress_ThrowsEmailSendException() {
        stubMimeMessage();
        var service = new SmtpEmailSender(mailSender, "");

        assertThrows(EmailSendException.class, () ->
                service.sendEmail("dest@example.com", "Hello", "<html></html>"));
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void sendEmail_InvalidRecipient_ThrowsEmailSendException() {
        stubMimeMessage();
        var service = new SmtpEmailSender(mailSender, "sender@example.com");

        assertThrows(EmailSendException.class, () ->
                service.sendEmail("not an address", "Hello", "<html></html>"));
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }
}
