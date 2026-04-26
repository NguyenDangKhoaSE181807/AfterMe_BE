package com.example.reminder.dto.reminder;

import com.example.reminder.domain.enums.TonePreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReminderRequest(
        Long habitId,
        @NotBlank @Size(max = 255) String title,
        String description,
        TonePreference tone,
        Boolean safetyEnabled
) {
}





