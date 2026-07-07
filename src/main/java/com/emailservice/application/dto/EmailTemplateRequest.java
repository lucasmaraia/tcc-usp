package com.emailservice.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailTemplateRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Subject is required")
        @Size(max = 255, message = "Subject must be at most 255 characters")
        String subject,

        @NotBlank(message = "HTML content is required")
        String htmlContent
) {}
