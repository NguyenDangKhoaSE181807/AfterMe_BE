package com.example.reminder.dto.reminder;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.TonePreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReminderRequest(
        @NotNull Long userId,
        Long habitId,
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull TonePreference tone,
        @NotNull Boolean safetyEnabled,
        @NotNull ReminderStatus status
) {
}





