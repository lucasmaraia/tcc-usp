package com.emailservice;

import com.emailservice.application.dto.EmailAttachmentDto;
import com.emailservice.application.dto.EmailMessageDto;
import com.emailservice.application.dto.SendEmailRequest;
import com.emailservice.application.port.EmailQueueGateway;
import com.emailservice.application.usecase.SendEmailUseCase;
import com.emailservice.domain.entity.EmailMessage;
import com.emailservice.domain.entity.EmailTemplate;
import com.emailservice.domain.entity.User;
import com.emailservice.domain.exception.BadRequestException;
import com.emailservice.domain.exception.ResourceNotFoundException;
import com.emailservice.domain.repository.EmailMessageRepository;
import com.emailservice.domain.repository.EmailTemplateRepository;
import com.emailservice.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendEmailUseCaseTest {

    @Mock
    private EmailTemplateRepository templateRepository;

    @Mock
    private EmailMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailQueueGateway emailQueueGateway;

    private SendEmailUseCase sendEmailUseCase;

    private User testUser;
    private EmailTemplate testTemplate;
    private UUID userId;
    private UUID templateId;
    private UUID messageId;

    @BeforeEach
    void setUp() {
        sendEmailUseCase = new SendEmailUseCase(
                templateRepository, messageRepository, userRepository,
                emailQueueGateway, JsonMapper.builder().build());

        userId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        messageId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .enabled(true)
                .build();

        testTemplate = EmailTemplate.builder()
                .id(templateId)
                .name("Test Template")
                .subject("Test Subject")
                .htmlContent("<html><body><h1>Hello [[${name}]]</h1></body></html>")
                .user(testUser)
                .build();
    }

    @Test
    void sendEmail_Success() {
        var request = new SendEmailRequest(templateId, "dest@example.com", Map.of("name", "Ana"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(testTemplate));
        when(messageRepository.save(any(EmailMessage.class))).thenAnswer(invocation -> {
            EmailMessage saved = invocation.getArgument(0);
            saved.setId(messageId);
            return saved;
        });

        var result = sendEmailUseCase.sendEmail(request, "testuser");

        assertNotNull(result);
        assertEquals(messageId, result.id());
        assertEquals("dest@example.com", result.toEmail());
        assertEquals("Test Subject", result.subject());
        assertEquals(EmailMessage.EmailStatus.PENDING.name(), result.status());
        assertEquals(0, result.retryCount());

        ArgumentCaptor<EmailMessageDto> dtoCaptor = ArgumentCaptor.forClass(EmailMessageDto.class);
        verify(emailQueueGateway).sendEmail(dtoCaptor.capture());
        EmailMessageDto dto = dtoCaptor.getValue();
        assertEquals(messageId, dto.id());
        assertEquals(templateId, dto.templateId());
        assertEquals("dest@example.com", dto.toEmail());
        assertEquals(testTemplate.getHtmlContent(), dto.htmlContent());
        assertEquals(Map.of("name", "Ana"), dto.variables());
        assertEquals(0, dto.retryCount());
    }

    @Test
    void sendEmail_WithAttachments_SanitizesFilenamesAndQueuesThem() {
        String base64 = Base64.getEncoder().encodeToString("conteudo".getBytes(StandardCharsets.UTF_8));
        var attachments = List.of(
                new EmailAttachmentDto("relatorio.pdf", "application/pdf", base64),
                new EmailAttachmentDto("../etc/passwd", "text/plain", base64));
        var request = new SendEmailRequest(templateId, "dest@example.com", Map.of(), attachments);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(testTemplate));
        when(messageRepository.save(any(EmailMessage.class))).thenAnswer(invocation -> {
            EmailMessage saved = invocation.getArgument(0);
            saved.setId(messageId);
            return saved;
        });

        var result = sendEmailUseCase.sendEmail(request, "testuser");

        assertEquals("relatorio.pdf, .._etc_passwd", result.attachmentNames());

        ArgumentCaptor<EmailMessageDto> dtoCaptor = ArgumentCaptor.forClass(EmailMessageDto.class);
        verify(emailQueueGateway).sendEmail(dtoCaptor.capture());
        List<EmailAttachmentDto> queued = dtoCaptor.getValue().attachments();
        assertEquals(2, queued.size());
        assertEquals("relatorio.pdf", queued.get(0).filename());
        assertEquals(".._etc_passwd", queued.get(1).filename());
        assertEquals(base64, queued.get(0).base64Content());
    }

    @Test
    void sendEmail_InvalidBase64Attachment_ThrowsBadRequest() {
        var attachments = List.of(new EmailAttachmentDto("arquivo.txt", "text/plain", "not-valid-base64!!!"));
        var request = new SendEmailRequest(templateId, "dest@example.com", Map.of(), attachments);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(testTemplate));

        assertThrows(BadRequestException.class, () ->
                sendEmailUseCase.sendEmail(request, "testuser")
        );
        verify(messageRepository, never()).save(any());
        verify(emailQueueGateway, never()).sendEmail(any());
    }

    @Test
    void sendEmail_AttachmentsOverSizeLimit_ThrowsBadRequest() {
        String bigBase64 = Base64.getEncoder().encodeToString(new byte[11 * 1024 * 1024]);
        var attachments = List.of(new EmailAttachmentDto("grande.bin", "application/octet-stream", bigBase64));
        var request = new SendEmailRequest(templateId, "dest@example.com", Map.of(), attachments);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(testTemplate));

        assertThrows(BadRequestException.class, () ->
                sendEmailUseCase.sendEmail(request, "testuser")
        );
        verify(emailQueueGateway, never()).sendEmail(any());
    }

    @Test
    void sendEmail_UserNotFound_ThrowsException() {
        var request = new SendEmailRequest(templateId, "dest@example.com", Map.of());

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                sendEmailUseCase.sendEmail(request, "ghost")
        );
        verify(emailQueueGateway, never()).sendEmail(any());
    }

    @Test
    void sendEmail_TemplateNotOwned_ThrowsException() {
        var request = new SendEmailRequest(templateId, "dest@example.com", Map.of());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByIdAndUserId(templateId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                sendEmailUseCase.sendEmail(request, "testuser")
        );
        verify(messageRepository, never()).save(any());
        verify(emailQueueGateway, never()).sendEmail(any());
    }

    @Test
    void getMessages_WithFilters_ReturnsPagedResponse() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(messageRepository.search(eq(userId), eq(EmailMessage.EmailStatus.SENT), eq("dest@"),
                pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(pendingMessage()), PageRequest.of(0, 10), 1));

        var result = sendEmailUseCase.getMessages("testuser", EmailMessage.EmailStatus.SENT, " dest@ ", 0, 10);

        assertEquals(1, result.content().size());
        assertEquals("dest@example.com", result.content().get(0).toEmail());
        assertEquals(1, result.totalElements());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getMessages_NoFilters_PassesNullsAndClampsPagination() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(messageRepository.search(eq(userId), isNull(), isNull(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));

        sendEmailUseCase.getMessages("testuser", null, "   ", -1, 999);

        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getMessageById_NotFound_ThrowsException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(messageRepository.findByIdAndUserId(messageId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                sendEmailUseCase.getMessageById(messageId, "testuser")
        );
    }

    @Test
    void markSent_UpdatesStatusHtmlBodyAndSentAt() {
        EmailMessage message = pendingMessage();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        sendEmailUseCase.markSent(messageId, "<html>rendered</html>");

        assertEquals(EmailMessage.EmailStatus.SENT, message.getStatus());
        assertEquals("<html>rendered</html>", message.getHtmlBody());
        assertNotNull(message.getSentAt());
        verify(messageRepository).save(message);
    }

    @Test
    void markRetrying_IncrementsRetryCount() {
        EmailMessage message = pendingMessage();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        sendEmailUseCase.markRetrying(messageId);

        assertEquals(EmailMessage.EmailStatus.RETRYING, message.getStatus());
        assertEquals(1, message.getRetryCount());
        verify(messageRepository).save(message);
    }

    @Test
    void markFailed_SetsStatusAndErrorMessage() {
        EmailMessage message = pendingMessage();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        sendEmailUseCase.markFailed(messageId, "SMTP unavailable");

        assertEquals(EmailMessage.EmailStatus.FAILED, message.getStatus());
        assertEquals("SMTP unavailable", message.getErrorMessage());
        assertNull(message.getSentAt());
        verify(messageRepository).save(message);
    }

    @Test
    void markSent_MessageNotFound_DoesNothing() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        sendEmailUseCase.markSent(messageId, "<html></html>");

        verify(messageRepository, never()).save(any());
    }

    @Test
    void isAlreadySent_DelegatesToRepository() {
        when(messageRepository.existsByIdAndStatus(messageId, EmailMessage.EmailStatus.SENT))
                .thenReturn(true).thenReturn(false);

        assertTrue(sendEmailUseCase.isAlreadySent(messageId));
        assertFalse(sendEmailUseCase.isAlreadySent(messageId));
    }

    private EmailMessage pendingMessage() {
        return EmailMessage.builder()
                .id(messageId)
                .userId(userId)
                .templateId(templateId)
                .templateName("Test Template")
                .toEmail("dest@example.com")
                .subject("Test Subject")
                .status(EmailMessage.EmailStatus.PENDING)
                .retryCount(0)
                .build();
    }
}
