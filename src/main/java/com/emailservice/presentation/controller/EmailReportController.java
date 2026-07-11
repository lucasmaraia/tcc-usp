package com.emailservice.presentation.controller;

import com.emailservice.application.dto.DailyEmailReportResponse;
import com.emailservice.application.dto.EmailReportItemResponse;
import com.emailservice.application.dto.EmailReportSummaryResponse;
import com.emailservice.application.dto.PageResponse;
import com.emailservice.application.usecase.EmailReportUseCase;
import com.emailservice.domain.entity.EmailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports/emails")
@RequiredArgsConstructor
public class EmailReportController {

    private final EmailReportUseCase reportUseCase;

    @GetMapping
    public ResponseEntity<PageResponse<EmailReportItemResponse>> getDetailedReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) EmailMessage.EmailStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reportUseCase.getDetailedReport(
                userDetails.getUsername(), startDate, endDate, status, page, size));
    }

    @GetMapping("/summary")
    public ResponseEntity<EmailReportSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reportUseCase.getSummary(userDetails.getUsername(), startDate, endDate));
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailyEmailReportResponse>> getDailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reportUseCase.getDailyReport(userDetails.getUsername(), startDate, endDate));
    }
}
