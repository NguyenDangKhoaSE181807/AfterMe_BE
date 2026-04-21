package com.example.reminder.dto.user;

import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.UserStatus;

public record CreateUserCommand(
        String email,
        String passwordHash,
        String fullName,
        TonePreference tonePreference,
        UserStatus status,
        UserRole role
) {
}
