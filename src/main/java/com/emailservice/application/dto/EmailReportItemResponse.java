package com.emailservice.application.dto;

import com.emailservice.domain.entity.EmailMessage;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailReportItemResponse(
        UUID id,
        String username,
        String userEmail,
        UUID templateId,
        String templateName,
        String toEmail,
        String subject,
        String status,
        int retryCount,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
    public static EmailReportItemResponse from(EmailMessage message, String username) {
        return new EmailReportItemResponse(
                message.getId(),
                username,
                message.getUserEmail(),
                message.getTemplateId(),
                message.getTemplateName(),
                message.getToEmail(),
                message.getSubject(),
                message.getStatus().name(),
                message.getRetryCount(),
                message.getSentAt(),
                message.getCreatedAt()
        );
    }
}
