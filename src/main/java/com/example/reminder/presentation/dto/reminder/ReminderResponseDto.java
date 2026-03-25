package com.example.reminder.presentation.dto.reminder;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.TonePreference;
import java.time.LocalDateTime;

public record ReminderResponseDto(
        Long id,
        Long userId,
        Long habitId,
        String title,
        String description,
        TonePreference tone,
        Boolean safetyEnabled,
        ReminderStatus status,
        LocalDateTime createdAt
) {
}





