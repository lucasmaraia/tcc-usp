package com.emailservice.application.usecase;

import com.emailservice.application.dto.EmailAttachmentDto;
import com.emailservice.application.dto.EmailMessageDto;
import com.emailservice.application.dto.EmailMessageResponse;
import com.emailservice.application.dto.PageResponse;
import com.emailservice.application.dto.SendEmailRequest;
import com.emailservice.application.port.EmailQueueGateway;
import com.emailservice.domain.entity.EmailMessage;
import com.emailservice.domain.entity.EmailTemplate;
import com.emailservice.domain.entity.User;
import com.emailservice.domain.exception.BadRequestException;
import com.emailservice.domain.exception.ResourceNotFoundException;
import com.emailservice.domain.repository.EmailMessageRepository;
import com.emailservice.domain.repository.EmailTemplateRepository;
import com.emailservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SendEmailUseCase {

    private static final long MAX_TOTAL_ATTACHMENT_BYTES = 10L * 1024 * 1024;
    private static final int MAX_PAGE_SIZE = 100;

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

        List<EmailAttachmentDto> attachments = sanitizeAttachments(request.attachments());

        EmailMessage message = EmailMessage.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .templateId(template.getId())
                .templateName(template.getName())
                .toEmail(request.toEmail())
                .subject(template.getSubject())
                .variablesJson(jsonMapper.writeValueAsString(request.variables()))
                .attachmentNames(attachments.isEmpty() ? null : attachments.stream()
                        .map(EmailAttachmentDto::filename)
                        .collect(Collectors.joining(", ")))
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
                attachments,
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

    public PageResponse<EmailMessageResponse> getMessages(String username, EmailMessage.EmailStatus status,
                                                          String toEmail, int page, int size) {
        User user = findUser(username);
        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        String recipient = toEmail == null || toEmail.isBlank() ? null : toEmail.trim();
        return PageResponse.from(messageRepository.search(user.getId(), status, recipient, pageable)
                .map(EmailMessageResponse::from));
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

    private List<EmailAttachmentDto> sanitizeAttachments(List<EmailAttachmentDto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        long totalBytes = 0;
        List<EmailAttachmentDto> sanitized = new ArrayList<>();

        for (EmailAttachmentDto attachment : attachments) {
            byte[] content;
            try {
                content = Base64.getDecoder().decode(attachment.base64Content());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(
                        "Attachment '" + attachment.filename() + "' has invalid Base64 content");
            }

            totalBytes += content.length;
            if (totalBytes > MAX_TOTAL_ATTACHMENT_BYTES) {
                throw new BadRequestException("Attachments exceed the total size limit of 10 MB");
            }

            String safeFilename = attachment.filename().trim().replaceAll("[\\\\/]", "_");
            sanitized.add(new EmailAttachmentDto(safeFilename, attachment.contentType(),
                    attachment.base64Content()));
        }

        return List.copyOf(sanitized);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
