package com.emailservice.application.dto;

import com.emailservice.domain.entity.EmailMessage;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailMessageResponse(
        UUID id,
        UUID userId,
        String userEmail,
        UUID templateId,
        String templateName,
        String toEmail,
        String subject,
        String status,
        String attachmentNames,
        int retryCount,
        String errorMessage,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
    public static EmailMessageResponse from(EmailMessage message) {
        return new EmailMessageResponse(
                message.getId(),
                message.getUserId(),
                message.getUserEmail(),
                message.getTemplateId(),
                message.getTemplateName(),
                message.getToEmail(),
                message.getSubject(),
                message.getStatus().name(),
                message.getAttachmentNames(),
                message.getRetryCount(),
                message.getErrorMessage(),
                message.getSentAt(),
                message.getCreatedAt()
        );
    }
}
