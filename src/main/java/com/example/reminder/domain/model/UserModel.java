package com.example.reminder.domain.model;

import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.domain.enums.UserStatus;
import java.time.LocalDateTime;

public record UserModel(
        Long id,
        String email,
        String passwordHash,
        String fullName,
        TonePreference tonePreference,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
}




