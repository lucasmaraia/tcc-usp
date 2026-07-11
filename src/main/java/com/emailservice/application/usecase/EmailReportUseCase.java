package com.emailservice.application.usecase;

import com.emailservice.application.dto.DailyEmailReportResponse;
import com.emailservice.application.dto.EmailReportItemResponse;
import com.emailservice.application.dto.EmailReportSummaryResponse;
import com.emailservice.application.dto.PageResponse;
import com.emailservice.domain.entity.EmailMessage;
import com.emailservice.domain.entity.User;
import com.emailservice.domain.exception.BadRequestException;
import com.emailservice.domain.exception.ResourceNotFoundException;
import com.emailservice.domain.repository.EmailMessageRepository;
import com.emailservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailReportUseCase {

    private static final int DEFAULT_PERIOD_DAYS = 30;
    private static final int MAX_PAGE_SIZE = 100;

    private final EmailMessageRepository messageRepository;
    private final UserRepository userRepository;

    public EmailReportSummaryResponse getSummary(String username, LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate);
        List<EmailMessage> messages = findMessages(username, range);

        long total = messages.size();
        long sent = countByStatus(messages, EmailMessage.EmailStatus.SENT);
        long failed = countByStatus(messages, EmailMessage.EmailStatus.FAILED);
        long pending = countByStatus(messages, EmailMessage.EmailStatus.PENDING);
        long retrying = countByStatus(messages, EmailMessage.EmailStatus.RETRYING);
        double successRate = total == 0 ? 0.0 : Math.round(sent * 10000.0 / total) / 100.0;

        return new EmailReportSummaryResponse(range.start(), range.end(),
                total, sent, failed, pending, retrying, successRate);
    }

    public List<DailyEmailReportResponse> getDailyReport(String username, LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate);
        List<EmailMessage> messages = findMessages(username, range);

        Map<LocalDate, List<EmailMessage>> byDay = messages.stream()
                .collect(Collectors.groupingBy(message -> message.getCreatedAt().toLocalDate(),
                        TreeMap::new, Collectors.toList()));

        return byDay.entrySet().stream()
                .map(entry -> new DailyEmailReportResponse(
                        entry.getKey(),
                        entry.getValue().size(),
                        countByStatus(entry.getValue(), EmailMessage.EmailStatus.SENT),
                        countByStatus(entry.getValue(), EmailMessage.EmailStatus.FAILED),
                        countByStatus(entry.getValue(), EmailMessage.EmailStatus.PENDING),
                        countByStatus(entry.getValue(), EmailMessage.EmailStatus.RETRYING)))
                .toList();
    }

    public PageResponse<EmailReportItemResponse> getDetailedReport(String username, LocalDate startDate,
                                                                   LocalDate endDate, EmailMessage.EmailStatus status,
                                                                   int page, int size) {
        DateRange range = resolveRange(startDate, endDate);
        User user = findUser(username);

        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<EmailMessage> messages = status == null
                ? messageRepository.findByUserIdAndCreatedAtBetween(user.getId(),
                        range.start().atStartOfDay(), range.end().atTime(LocalTime.MAX), pageable)
                : messageRepository.findByUserIdAndStatusAndCreatedAtBetween(user.getId(), status,
                        range.start().atStartOfDay(), range.end().atTime(LocalTime.MAX), pageable);

        return PageResponse.from(messages.map(message -> EmailReportItemResponse.from(message, user.getUsername())));
    }

    private List<EmailMessage> findMessages(String username, DateRange range) {
        User user = findUser(username);
        return messageRepository.findByUserIdAndCreatedAtBetween(user.getId(),
                range.start().atStartOfDay(), range.end().atTime(LocalTime.MAX));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private DateRange resolveRange(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(DEFAULT_PERIOD_DAYS - 1L);
        if (start.isAfter(end)) {
            throw new BadRequestException("Start date must not be after end date");
        }
        return new DateRange(start, end);
    }

    private static long countByStatus(List<EmailMessage> messages, EmailMessage.EmailStatus status) {
        return messages.stream().filter(message -> message.getStatus() == status).count();
    }

    private record DateRange(LocalDate start, LocalDate end) {}
}
