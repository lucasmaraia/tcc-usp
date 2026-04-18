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
public class EmailTemplateResponse {
    private UUID id;
    private String name;
    private String subject;
    private String htmlContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
