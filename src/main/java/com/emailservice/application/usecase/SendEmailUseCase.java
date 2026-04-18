package com.emailservice.application.usecase;

import com.emailservice.application.dto.EmailMessageDto;
import com.emailservice.application.dto.EmailMessageResponse;
import com.emailservice.application.dto.SendEmailRequest;
import com.emailservice.domain.entity.EmailMessage;
import com.emailservice.domain.entity.EmailTemplate;
import com.emailservice.domain.exception.ResourceNotFoundException;
import com.emailservice.domain.repository.EmailMessageRepository;
import com.emailservice.domain.repository.EmailTemplateRepository;
import com.emailservice.infrastructure.messaging.EmailMessageSender;
import com.emailservice.infrastructure.security.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SendEmailUseCase {

    private final EmailTemplateRepository templateRepository;
    private final EmailMessageRepository messageRepository;
    private final EmailMessageSender emailMessageSender;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    @Transactional
    public EmailMessageResponse sendEmail(SendEmailRequest request, String token) {
        UUID userId = jwtService.extractUserId(token);
        String userEmail = jwtService.extractEmail(token);

        EmailTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        String variablesJson;
        try {
            variablesJson = objectMapper.writeValueAsString(request.getVariables());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize variables", e);
        }

        EmailMessage message = EmailMessage.builder()
                .userId(userId)
                .userEmail(userEmail)
                .templateId(template.getId())
                .templateName(template.getName())
                .toEmail(request.getToEmail())
                .subject(template.getSubject())
                .variablesJson(variablesJson)
                .status(EmailMessage.EmailStatus.PENDING)
                .retryCount(0)
                .build();

        message = messageRepository.save(message);

        EmailMessageDto dto = EmailMessageDto.builder()
                .id(message.getId())
                .userId(userId)
                .templateId(template.getId())
                .templateName(template.getName())
                .toEmail(request.getToEmail())
                .subject(template.getSubject())
                .htmlContent(template.getHtmlContent())
                .variables(request.getVariables())
                .retryCount(0)
                .build();

        emailMessageSender.sendEmail(dto);

        return toResponse(message);
    }

    public EmailMessageResponse getMessageById(UUID id) {
        EmailMessage message = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email message not found"));
        return toResponse(message);
    }

    public List<EmailMessageResponse> getMessagesByStatus(EmailMessage.EmailStatus status) {
        return messageRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EmailMessageResponse> getMessagesByEmail(String email) {
        return messageRepository.findByToEmail(email).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateMessageStatus(UUID id, EmailMessage.EmailStatus status, String errorMessage) {
        EmailMessage message = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email message not found"));
        
        message.setStatus(status);
        if (errorMessage != null) {
            message.setErrorMessage(errorMessage);
        }
        
        if (status == EmailMessage.EmailStatus.SENT) {
            message.setSentAt(java.time.LocalDateTime.now());
        }
        
        messageRepository.save(message);
    }

    @Transactional
    public void incrementRetryCount(UUID id) {
        EmailMessage message = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email message not found"));
        message.setRetryCount(message.getRetryCount() + 1);
        messageRepository.save(message);
    }

    private EmailMessageResponse toResponse(EmailMessage message) {
        return EmailMessageResponse.builder()
                .id(message.getId())
                .userId(message.getUserId())
                .userEmail(message.getUserEmail())
                .templateId(message.getTemplateId())
                .templateName(message.getTemplateName())
                .toEmail(message.getToEmail())
                .subject(message.getSubject())
                .status(message.getStatus().name())
                .retryCount(message.getRetryCount())
                .errorMessage(message.getErrorMessage())
                .sentAt(message.getSentAt())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
