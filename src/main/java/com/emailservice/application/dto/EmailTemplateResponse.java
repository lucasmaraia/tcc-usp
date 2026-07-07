package com.emailservice.application.dto;

import com.emailservice.domain.entity.EmailTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailTemplateResponse(
        UUID id,
        String name,
        String subject,
        String htmlContent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EmailTemplateResponse from(EmailTemplate template) {
        return new EmailTemplateResponse(
                template.getId(),
                template.getName(),
                template.getSubject(),
                template.getHtmlContent(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
