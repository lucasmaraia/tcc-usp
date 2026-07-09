package com.emailservice;

import com.emailservice.application.dto.EmailTemplateRequest;
import com.emailservice.application.port.TemplateRenderer;
import com.emailservice.application.usecase.EmailTemplateUseCase;
import com.emailservice.domain.entity.EmailTemplate;
import com.emailservice.domain.entity.User;
import com.emailservice.domain.exception.BadRequestException;
import com.emailservice.domain.exception.ResourceNotFoundException;
import com.emailservice.domain.repository.EmailTemplateRepository;
import com.emailservice.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailTemplateUseCaseTest {

    @Mock
    private EmailTemplateRepository templateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TemplateRenderer templateRenderer;

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
                .user(testUser)
                .build();
    }

    @Test
    void createTemplate_Success() {
        var request = new EmailTemplateRequest("New Template", "New Subject", "<html><body>New Content</body></html>");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.existsByNameAndUserId("New Template", userId)).thenReturn(false);
        when(templateRepository.save(any(EmailTemplate.class))).thenAnswer(invocation -> {
            EmailTemplate saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var result = templateUseCase.createTemplate(request, "testuser");

        assertNotNull(result);
        assertEquals("New Template", result.name());
        assertEquals("New Subject", result.subject());
        verify(templateRepository).save(any(EmailTemplate.class));
    }

    @Test
    void createTemplate_DuplicateName_ThrowsException() {
        var request = new EmailTemplateRequest("Existing Template", "Subject", "Content");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.existsByNameAndUserId("Existing Template", userId)).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                templateUseCase.createTemplate(request, "testuser")
        );
    }

    @Test
    void getTemplateById_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(testTemplate));

        var result = templateUseCase.getTemplateById(templateId, "testuser");

        assertNotNull(result);
        assertEquals(templateId, result.id());
        assertEquals("Test Template", result.name());
    }

    @Test
    void getTemplateById_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByIdAndUserId(nonExistentId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                templateUseCase.getTemplateById(nonExistentId, "testuser")
        );
    }

    @Test
    void deleteTemplate_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(testTemplate));

        templateUseCase.deleteTemplate(templateId, "testuser");

        verify(templateRepository).delete(testTemplate);
    }

    @Test
    void getTemplateByName_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByNameAndUserId("Test Template", userId)).thenReturn(Optional.of(testTemplate));

        var result = templateUseCase.getTemplateByName("Test Template", "testuser");

        assertNotNull(result);
        assertEquals(templateId, result.id());
    }

    @Test
    void getTemplateByName_NotFound_ThrowsException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByNameAndUserId("Missing", userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                templateUseCase.getTemplateByName("Missing", "testuser")
        );
    }

    @Test
    void getTemplatesByUser_ReturnsAllTemplates() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(testTemplate));

        var result = templateUseCase.getTemplatesByUser("testuser");

        assertEquals(1, result.size());
        assertEquals("Test Template", result.get(0).name());
    }

    @Test
    void getTemplatesByUser_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                templateUseCase.getTemplatesByUser("ghost")
        );
    }

    @Test
    void updateTemplate_Success() {
        var request = new EmailTemplateRequest("Renamed Template", "New Subject", "<p>Updated</p>");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(testTemplate));
        when(templateRepository.existsByNameAndUserId("Renamed Template", userId)).thenReturn(false);
        when(templateRepository.save(testTemplate)).thenReturn(testTemplate);

        var result = templateUseCase.updateTemplate(templateId, request, "testuser");

        assertEquals("Renamed Template", result.name());
        assertEquals("New Subject", result.subject());
        assertEquals("<p>Updated</p>", result.htmlContent());
    }

    @Test
    void updateTemplate_SameName_DoesNotCheckDuplicate() {
        var request = new EmailTemplateRequest("Test Template", "New Subject", "<p>Updated</p>");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(testTemplate));
        when(templateRepository.save(testTemplate)).thenReturn(testTemplate);

        var result = templateUseCase.updateTemplate(templateId, request, "testuser");

        assertEquals("New Subject", result.subject());
        verify(templateRepository, never()).existsByNameAndUserId(any(), any());
    }

    @Test
    void updateTemplate_DuplicateNewName_ThrowsException() {
        var request = new EmailTemplateRequest("Taken Name", "Subject", "Content");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(templateRepository.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(testTemplate));
        when(templateRepository.existsByNameAndUserId("Taken Name", userId)).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                templateUseCase.updateTemplate(templateId, request, "testuser")
        );
        verify(templateRepository, never()).save(any());
    }

    @Test
    void renderPreview_DelegatesToRenderer() {
        Map<String, Object> variables = Map.of("name", "Ana");
        when(templateRenderer.render("<p>[[${name}]]</p>", variables)).thenReturn("<p>Ana</p>");

        String result = templateUseCase.renderPreview("<p>[[${name}]]</p>", variables);

        assertEquals("<p>Ana</p>", result);
    }

    @Test
    void deleteTemplate_NotOwned_ThrowsNotFound() {
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder()
                .id(otherUserId)
                .username("otheruser")
                .build();

        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));
        when(templateRepository.findByIdAndUserId(templateId, otherUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                templateUseCase.deleteTemplate(templateId, "otheruser")
        );
    }
}
