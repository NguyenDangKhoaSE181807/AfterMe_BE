package com.example.reminder.dto.user;

import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.UserStatus;
import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String email,
        String fullName,
        TonePreference tonePreference,
        UserStatus status,
        UserRole role,
        LocalDateTime createdAt
) {
}





