package com.emailservice;

import com.emailservice.application.usecase.EmailTemplateUseCase;
import com.emailservice.domain.entity.EmailTemplate;
import com.emailservice.domain.entity.User;
import com.emailservice.domain.exception.BadRequestException;
import com.emailservice.domain.repository.EmailTemplateRepository;
import com.emailservice.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTemplateUseCaseTest {

    @Mock
    private EmailTemplateRepository templateRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmailTemplateUseCase templateUseCase;

    private User testUser;
    private EmailTemplate testTemplate;
    private UUID userId;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        templateId = UUID.randomUUID();

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
                .htmlContent("Hello ${name}")
                .user(testUser)
                .build();
    }

    @Test
    void createTemplate_Success() {
        var request = new com.emailservice.application.dto.EmailTemplateRequest();
        request.setName("New Template");
        request.setSubject("New Subject");
        request.setHtmlContent("<html><body>New Content</body></html>");

        when(templateRepository.existsByName("New Template")).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.save(any(EmailTemplate.class))).thenAnswer(invocation -> {
            EmailTemplate saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var result = templateUseCase.createTemplate(request, "testuser");

        assertNotNull(result);
        assertEquals("New Template", result.getName());
        assertEquals("New Subject", result.getSubject());
        verify(templateRepository).save(any(EmailTemplate.class));
    }

    @Test
    void createTemplate_DuplicateName_ThrowsException() {
        var request = new com.emailservice.application.dto.EmailTemplateRequest();
        request.setName("Existing Template");
        request.setSubject("Subject");
        request.setHtmlContent("Content");

        when(templateRepository.existsByName("Existing Template")).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
            templateUseCase.createTemplate(request, "testuser")
        );
    }

    @Test
    void getTemplateById_Success() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));

        var result = templateUseCase.getTemplateById(templateId);

        assertNotNull(result);
        assertEquals(templateId, result.getId());
        assertEquals("Test Template", result.getName());
    }

    @Test
    void getTemplateById_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(templateRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(com.emailservice.domain.exception.ResourceNotFoundException.class, () ->
            templateUseCase.getTemplateById(nonExistentId)
        );
    }

    @Test
    void deleteTemplate_Success() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        templateUseCase.deleteTemplate(templateId, "testuser");

        verify(templateRepository).delete(testTemplate);
    }

    @Test
    void deleteTemplate_Unauthorized_ThrowsException() {
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("otheruser")
                .build();

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));

        assertThrows(BadRequestException.class, () ->
            templateUseCase.deleteTemplate(templateId, "otheruser")
        );
    }
}
