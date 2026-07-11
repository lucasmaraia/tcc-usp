package com.emailservice;

import com.emailservice.application.usecase.EmailReportUseCase;
import com.emailservice.domain.entity.EmailMessage;
import com.emailservice.domain.entity.User;
import com.emailservice.domain.exception.BadRequestException;
import com.emailservice.domain.exception.ResourceNotFoundException;
import com.emailservice.domain.repository.EmailMessageRepository;
import com.emailservice.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailReportUseCaseTest {

    @Mock
    private EmailMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmailReportUseCase reportUseCase;

    private User testUser;
    private UUID userId;
    private final LocalDate startDate = LocalDate.of(2026, 7, 1);
    private final LocalDate endDate = LocalDate.of(2026, 7, 8);

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .enabled(true)
                .build();
    }

    @Test
    void getSummary_CountsMessagesByStatus() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(messageRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                .thenReturn(List.of(
                        message(EmailMessage.EmailStatus.SENT, startDate),
                        message(EmailMessage.EmailStatus.SENT, startDate.plusDays(1)),
                        message(EmailMessage.EmailStatus.SENT, startDate.plusDays(2)),
                        message(EmailMessage.EmailStatus.FAILED, startDate.plusDays(2)),
                        message(EmailMessage.EmailStatus.PENDING, startDate.plusDays(3)),
                        message(EmailMessage.EmailStatus.RETRYING, startDate.plusDays(3))
                ));

        var result = reportUseCase.getSummary("testuser", startDate, endDate);

        assertEquals(startDate, result.startDate());
        assertEquals(endDate, result.endDate());
        assertEquals(6, result.total());
        assertEquals(3, result.sent());
        assertEquals(1, result.failed());
        assertEquals(1, result.pending());
        assertEquals(1, result.retrying());
        assertEquals(50.0, result.successRate());
    }

    @Test
    void getSummary_NoMessages_ReturnsZeroedReport() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(messageRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                .thenReturn(List.of());

        var result = reportUseCase.getSummary("testuser", startDate, endDate);

        assertEquals(0, result.total());
        assertEquals(0.0, result.successRate());
    }

    @Test
    void getSummary_QueriesFullDaysOfThePeriod() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(messageRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                .thenReturn(List.of());

        reportUseCase.getSummary("testuser", startDate, endDate);

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(messageRepository).findByUserIdAndCreatedAtBetween(eq(userId), fromCaptor.capture(), toCaptor.capture());
        assertEquals(startDate.atStartOfDay(), fromCaptor.getValue());
        assertEquals(endDate.atTime(LocalTime.MAX), toCaptor.getValue());
    }

    @Test
    void getSummary_WithoutDates_DefaultsToLast30Days() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(messageRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                .thenReturn(List.of());

        var result = reportUseCase.getSummary("testuser", null, null);

        assertEquals(LocalDate.now(), result.endDate());
        assertEquals(LocalDate.now().minusDays(29), result.startDate());
    }

    @Test
    void getSummary_StartAfterEnd_ThrowsException() {
        assertThrows(BadRequestException.class, () ->
                reportUseCase.getSummary("testuser", endDate, startDate)
        );
    }

    @Test
    void getSummary_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reportUseCase.getSummary("ghost", startDate, endDate)
        );
    }

    @Test
    void getDailyReport_GroupsByDayInChronologicalOrder() {
        LocalDate day1 = startDate;
        LocalDate day2 = startDate.plusDays(3);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(messageRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                .thenReturn(List.of(
                        message(EmailMessage.EmailStatus.SENT, day2),
                        message(EmailMessage.EmailStatus.SENT, day1),
                        message(EmailMessage.EmailStatus.FAILED, day1)
                ));

        var result = reportUseCase.getDailyReport("testuser", startDate, endDate);

        assertEquals(2, result.size());
        assertEquals(day1, result.get(0).date());
        assertEquals(2, result.get(0).total());
        assertEquals(1, result.get(0).sent());
        assertEquals(1, result.get(0).failed());
        assertEquals(day2, result.get(1).date());
        assertEquals(1, result.get(1).total());
        assertEquals(1, result.get(1).sent());
    }

    @Test
    void getDailyReport_NoMessages_ReturnsEmptyList() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(messageRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                .thenReturn(List.of());

        var result = reportUseCase.getDailyReport("testuser", startDate, endDate);

        assertTrue(result.isEmpty());
    }

    @Test
    void getDetailedReport_ReturnsPaginatedItemsWithUserInfo() {
        EmailMessage message = message(EmailMessage.EmailStatus.SENT, startDate);
        message.setSentAt(startDate.atTime(10, 5));
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(messageRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(message), pageable, 45));

        var result = reportUseCase.getDetailedReport("testuser", startDate, endDate, null, 0, 20);

        assertEquals(0, result.page());
        assertEquals(20, result.size());
        assertEquals(45, result.totalElements());
        assertEquals(3, result.totalPages());
        assertEquals(1, result.content().size());

        var item = result.content().get(0);
        assertEquals(message.getId(), item.id());
        assertEquals("testuser", item.username());
        assertEquals("Test Template", item.templateName());
        assertEquals("dest@example.com", item.toEmail());
        assertEquals("SENT", item.status());
        assertEquals(startDate.atTime(10, 5), item.sentAt());
        assertEquals(startDate.atTime(10, 0), item.createdAt());
    }

    @Test
    void getDetailedReport_WithStatus_UsesStatusFilteredQuery() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(messageRepository.findByUserIdAndStatusAndCreatedAtBetween(
                eq(userId), eq(EmailMessage.EmailStatus.FAILED), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = reportUseCase.getDetailedReport("testuser", startDate, endDate,
                EmailMessage.EmailStatus.FAILED, 0, 20);

        assertEquals(0, result.totalElements());
        verify(messageRepository).findByUserIdAndStatusAndCreatedAtBetween(
                eq(userId), eq(EmailMessage.EmailStatus.FAILED), any(), any(), any(Pageable.class));
    }

    @Test
    void getDetailedReport_NormalizesInvalidPagination() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(messageRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        reportUseCase.getDetailedReport("testuser", startDate, endDate, null, -5, 5000);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findByUserIdAndCreatedAtBetween(eq(userId), any(), any(), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageableCaptor.getValue().getSort());
    }

    private EmailMessage message(EmailMessage.EmailStatus status, LocalDate day) {
        return EmailMessage.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .templateId(UUID.randomUUID())
                .templateName("Test Template")
                .toEmail("dest@example.com")
                .subject("Test Subject")
                .status(status)
                .retryCount(0)
                .createdAt(day.atTime(10, 0))
                .build();
    }
}
