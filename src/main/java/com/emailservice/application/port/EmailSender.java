package com.emailservice.application.port;

import com.emailservice.application.dto.EmailAttachmentDto;

import java.util.List;

public interface EmailSender {

    default void sendEmail(String to, String subject, String htmlBody) {
        sendEmail(to, subject, htmlBody, List.of());
    }

    void sendEmail(String to, String subject, String htmlBody, List<EmailAttachmentDto> attachments);
}
