package com.example.reminder.domain.model;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.TonePreference;
import java.time.LocalDateTime;

public record ReminderModel(
        Long id,
        Long userId,
        Long habitId,
        String title,
        String description,
        TonePreference tone,
        Boolean safetyEnabled,
        ReminderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}




