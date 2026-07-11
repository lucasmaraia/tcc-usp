package com.emailservice.application.dto;

import java.time.LocalDate;

public record EmailReportSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        long total,
        long sent,
        long failed,
        long pending,
        long retrying,
        double successRate
) {}
