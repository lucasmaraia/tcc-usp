package com.emailservice.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {
    private UUID userId;

    @NotNull(message = "Template ID is required")
    private UUID templateId;

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String toEmail;

    @NotNull(message = "Variables are required")
    private Map<String, Object> variables;
}
