package com.example.reminder.dto.trustedcontact;

import java.time.LocalDateTime;

public record TrustedContactResponseDto(
        Long id,
        Long userId,
        String fullName,
        String relationship,
        String phone,
        String email,
        Boolean isActive,
        LocalDateTime createdAt
) {
}
