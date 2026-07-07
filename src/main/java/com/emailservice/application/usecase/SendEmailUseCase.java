package com.emailservice.application.usecase;

import com.emailservice.application.dto.EmailMessageDto;
import com.emailservice.application.dto.EmailMessageResponse;
import com.emailservice.application.dto.SendEmailRequest;
import com.emailservice.application.port.EmailQueueGateway;
import com.emailservice.domain.entity.EmailMessage;
import com.emailservice.domain.entity.EmailTemplate;
import com.emailservice.domain.entity.User;
import com.emailservice.domain.exception.ResourceNotFoundException;
import com.emailservice.domain.repository.EmailMessageRepository;
import com.emailservice.domain.repository.EmailTemplateRepository;
import com.emailservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SendEmailUseCase {

    private final EmailTemplateRepository templateRepository;
    private final EmailMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final EmailQueueGateway emailQueueGateway;
    private final JsonMapper jsonMapper;

    @Transactional
    public EmailMessageResponse sendEmail(SendEmailRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        EmailTemplate template = templateRepository.findByIdAndUserId(request.templateId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        EmailMessage message = EmailMessage.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .templateId(template.getId())
                .templateName(template.getName())
                .toEmail(request.toEmail())
                .subject(template.getSubject())
                .variablesJson(jsonMapper.writeValueAsString(request.variables()))
                .status(EmailMessage.EmailStatus.PENDING)
                .retryCount(0)
                .build();

        message = messageRepository.save(message);

        EmailMessageDto dto = new EmailMessageDto(
                message.getId(),
                user.getId(),
                template.getId(),
                template.getName(),
                request.toEmail(),
                template.getSubject(),
                template.getHtmlContent(),
                request.variables(),
                0
        );

        emailQueueGateway.sendEmail(dto);

        return EmailMessageResponse.from(message);
    }

    public EmailMessageResponse getMessageById(UUID id, String username) {
        User user = findUser(username);
        EmailMessage message = messageRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Email message not found"));
        return EmailMessageResponse.from(message);
    }

    public List<EmailMessageResponse> getMessages(String username, EmailMessage.EmailStatus status) {
        User user = findUser(username);
        List<EmailMessage> messages = status == null
                ? messageRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                : messageRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status);
        return messages.stream().map(EmailMessageResponse::from).toList();
    }

    @Transactional
    public void markSent(UUID id, String htmlBody) {
        messageRepository.findById(id).ifPresent(message -> {
            message.setStatus(EmailMessage.EmailStatus.SENT);
            message.setHtmlBody(htmlBody);
            message.setSentAt(LocalDateTime.now());
            messageRepository.save(message);
        });
    }

    @Transactional
    public void markRetrying(UUID id) {
        messageRepository.findById(id).ifPresent(message -> {
            message.setStatus(EmailMessage.EmailStatus.RETRYING);
            message.setRetryCount(message.getRetryCount() + 1);
            messageRepository.save(message);
        });
    }

    @Transactional
    public void markFailed(UUID id, String errorMessage) {
        messageRepository.findById(id).ifPresent(message -> {
            message.setStatus(EmailMessage.EmailStatus.FAILED);
            message.setErrorMessage(errorMessage);
            messageRepository.save(message);
        });
    }

    public boolean isAlreadySent(UUID id) {
        return messageRepository.existsByIdAndStatus(id, EmailMessage.EmailStatus.SENT);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
