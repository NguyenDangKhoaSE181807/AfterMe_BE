package com.example.reminder.dto.auth;

import com.example.reminder.domain.enums.UserRole;

public record CookieAuthResponseDto(
        Long userId,
        String email,
        UserRole role,
        String accessToken,
        String message
) {}
