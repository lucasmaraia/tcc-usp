package com.emailservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessageResponse {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private UUID templateId;
    private String templateName;
    private String toEmail;
    private String subject;
    private String status;
    private int retryCount;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
