package com.emailservice.infrastructure.service;

import com.emailservice.application.dto.EmailAttachmentDto;
import com.emailservice.application.port.EmailSender;
import com.emailservice.domain.exception.EmailSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
public class SmtpEmailSender implements EmailSender {

    private static final String DEFAULT_ATTACHMENT_TYPE = "application/octet-stream";

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${spring.mail.username}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendEmail(String to, String subject, String htmlBody, List<EmailAttachmentDto> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (attachments != null) {
                for (EmailAttachmentDto attachment : attachments) {
                    byte[] content = Base64.getDecoder().decode(attachment.base64Content());
                    String contentType = attachment.contentType() == null || attachment.contentType().isBlank()
                            ? DEFAULT_ATTACHMENT_TYPE
                            : attachment.contentType();
                    helper.addAttachment(attachment.filename(), new ByteArrayResource(content), contentType);
                }
            }

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);

        } catch (MessagingException | IllegalArgumentException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new EmailSendException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
