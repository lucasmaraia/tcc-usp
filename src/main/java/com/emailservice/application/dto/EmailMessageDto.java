package com.emailservice.application.dto;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Payload exchanged with the message broker.
 */
public record EmailMessageDto(
        UUID id,
        UUID userId,
        UUID templateId,
        String templateName,
        String toEmail,
        String subject,
        String htmlContent,
        Map<String, Object> variables,
        int retryCount
) implements Serializable {

    public EmailMessageDto withRetryCount(int newRetryCount) {
        return new EmailMessageDto(id, userId, templateId, templateName, toEmail,
                subject, htmlContent, variables, newRetryCount);
    }
}
