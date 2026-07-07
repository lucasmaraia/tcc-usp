package com.emailservice.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record SendEmailRequest(

        @NotNull(message = "Template ID is required")
        UUID templateId,

        @NotBlank(message = "Recipient email is required")
        @Email(message = "Invalid email format")
        String toEmail,

        @NotNull(message = "Variables are required")
        Map<String, Object> variables
) {}
