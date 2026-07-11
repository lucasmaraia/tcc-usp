package com.emailservice.application.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EmailMessageDto(
        UUID id,
        UUID userId,
        UUID templateId,
        String templateName,
        String toEmail,
        String subject,
        String htmlContent,
        Map<String, Object> variables,
        List<EmailAttachmentDto> attachments,
        int retryCount
) implements Serializable {

    public EmailMessageDto withRetryCount(int newRetryCount) {
        return new EmailMessageDto(id, userId, templateId, templateName, toEmail,
                subject, htmlContent, variables, attachments, newRetryCount);
    }
}
