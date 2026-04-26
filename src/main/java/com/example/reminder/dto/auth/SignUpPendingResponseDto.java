package com.example.reminder.dto.auth;

public record SignUpPendingResponseDto(
        Long userId,
        String email,
        String message
) {
}
