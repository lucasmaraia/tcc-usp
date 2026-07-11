package com.emailservice.application.dto;

import java.time.LocalDate;

public record DailyEmailReportResponse(
        LocalDate date,
        long total,
        long sent,
        long failed,
        long pending,
        long retrying
) {}
