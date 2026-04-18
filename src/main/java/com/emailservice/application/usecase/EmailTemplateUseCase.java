package com.emailservice.application.usecase;

import com.emailservice.application.dto.EmailTemplateRequest;
import com.emailservice.application.dto.EmailTemplateResponse;
import com.emailservice.domain.entity.EmailTemplate;
import com.emailservice.domain.entity.User;
import com.emailservice.domain.exception.BadRequestException;
import com.emailservice.domain.exception.ResourceNotFoundException;
import com.emailservice.domain.repository.EmailTemplateRepository;
import com.emailservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailTemplateUseCase {

    private final EmailTemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Transactional
    public EmailTemplateResponse createTemplate(EmailTemplateRequest request, String username) {
        if (templateRepository.existsByName(request.getName())) {
            throw new BadRequestException("Template name already exists");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        EmailTemplate template = EmailTemplate.builder()
                .name(request.getName())
                .subject(request.getSubject())
                .htmlContent(request.getHtmlContent())
                .user(user)
                .build();

        template = templateRepository.save(template);
        return toResponse(template);
    }

    public EmailTemplateResponse getTemplateById(UUID id) {
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        return toResponse(template);
    }

    public EmailTemplateResponse getTemplateByName(String name) {
        EmailTemplate template = templateRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        return toResponse(template);
    }

    public List<EmailTemplateResponse> getTemplatesByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return templateRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmailTemplateResponse updateTemplate(UUID id, EmailTemplateRequest request, String username) {
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!template.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to update this template");
        }

        if (!template.getName().equals(request.getName()) && 
            templateRepository.existsByNameAndUserId(request.getName(), user.getId())) {
            throw new BadRequestException("Template name already exists");
        }

        template.setName(request.getName());
        template.setSubject(request.getSubject());
        template.setHtmlContent(request.getHtmlContent());

        template = templateRepository.save(template);
        return toResponse(template);
    }

    @Transactional
    public void deleteTemplate(UUID id, String username) {
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!template.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to delete this template");
        }

        templateRepository.delete(template);
    }

    private EmailTemplateResponse toResponse(EmailTemplate template) {
        return EmailTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .subject(template.getSubject())
                .htmlContent(template.getHtmlContent())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
