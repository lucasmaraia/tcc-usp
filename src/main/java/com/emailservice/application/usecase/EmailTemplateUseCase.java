package com.emailservice.application.usecase;

import com.emailservice.application.dto.EmailTemplateRequest;
import com.emailservice.application.dto.EmailTemplateResponse;
import com.emailservice.application.port.TemplateRenderer;
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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailTemplateUseCase {

    private final EmailTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final TemplateRenderer templateRenderer;

    @Transactional
    public EmailTemplateResponse createTemplate(EmailTemplateRequest request, String username) {
        User user = findUser(username);

        if (templateRepository.existsByNameAndUserId(request.name(), user.getId())) {
            throw new BadRequestException("Template name already exists");
        }

        EmailTemplate template = EmailTemplate.builder()
                .name(request.name())
                .subject(request.subject())
                .htmlContent(request.htmlContent())
                .user(user)
                .build();

        return EmailTemplateResponse.from(templateRepository.save(template));
    }

    public EmailTemplateResponse getTemplateById(UUID id, String username) {
        return EmailTemplateResponse.from(findOwnedTemplate(id, username));
    }

    public EmailTemplateResponse getTemplateByName(String name, String username) {
        User user = findUser(username);
        EmailTemplate template = templateRepository.findByNameAndUserId(name, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        return EmailTemplateResponse.from(template);
    }

    public List<EmailTemplateResponse> getTemplatesByUser(String username) {
        User user = findUser(username);
        return templateRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(EmailTemplateResponse::from)
                .toList();
    }

    public String renderPreview(String htmlContent, Map<String, Object> variables) {
        return templateRenderer.render(htmlContent, variables);
    }

    @Transactional
    public EmailTemplateResponse updateTemplate(UUID id, EmailTemplateRequest request, String username) {
        EmailTemplate template = findOwnedTemplate(id, username);

        boolean nameChanged = !template.getName().equals(request.name());
        if (nameChanged && templateRepository.existsByNameAndUserId(request.name(), template.getUser().getId())) {
            throw new BadRequestException("Template name already exists");
        }

        template.setName(request.name());
        template.setSubject(request.subject());
        template.setHtmlContent(request.htmlContent());

        return EmailTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(UUID id, String username) {
        templateRepository.delete(findOwnedTemplate(id, username));
    }

    private EmailTemplate findOwnedTemplate(UUID id, String username) {
        User user = findUser(username);
        return templateRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
