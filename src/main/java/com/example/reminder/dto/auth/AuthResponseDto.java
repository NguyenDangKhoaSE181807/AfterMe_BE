package com.example.reminder.dto.auth;

import com.example.reminder.domain.enums.UserRole;

public record AuthResponseDto(
        String tokenType,
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        Long userId,
        String email,
        UserRole role
) {
}
