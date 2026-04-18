package com.emailservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessageDto implements Serializable {
    private UUID id;
    private UUID userId;
    private UUID templateId;
    private String templateName;
    private String toEmail;
    private String subject;
    private String htmlContent;
    private Map<String, Object> variables;
    private int retryCount;
}
