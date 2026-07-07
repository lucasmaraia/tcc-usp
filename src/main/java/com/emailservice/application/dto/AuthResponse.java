package com.emailservice.application.dto;

public record AuthResponse(
        String token,
        String username,
        String email
) {}
