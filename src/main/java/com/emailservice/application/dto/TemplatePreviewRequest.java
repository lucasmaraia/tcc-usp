package com.emailservice.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record TemplatePreviewRequest(

        @NotBlank(message = "HTML content is required")
        String html,

        Map<String, Object> variables
) {}
