package com.emailservice.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SendEmailRequest(

        @NotNull(message = "Template ID is required")
        UUID templateId,

        @NotBlank(message = "Recipient email is required")
        @Email(message = "Invalid email format")
        String toEmail,

        @NotNull(message = "Variables are required")
        Map<String, Object> variables,

        @Size(max = 5, message = "A maximum of 5 attachments is allowed")
        List<@Valid EmailAttachmentDto> attachments
) {
    public SendEmailRequest(UUID templateId, String toEmail, Map<String, Object> variables) {
        this(templateId, toEmail, variables, List.of());
    }
}
