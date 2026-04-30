package com.example.reminder.dto.reminder;

import com.example.reminder.domain.enums.TonePreference;

public record CreateReminderCommand(
        Long userId,
        Long habitId,
        String title,
        String description,
        TonePreference tone,
        Boolean safetyEnabled
) {
}
