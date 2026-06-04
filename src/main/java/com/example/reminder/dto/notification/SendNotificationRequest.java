package com.example.reminder.dto.notification;

import com.example.reminder.domain.enums.ReminderSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendNotificationRequest(
        @NotNull Long userId,
        @NotBlank String title,
        @NotBlank String body,
        Long reminderId,
        Long scheduleId,
        Long instanceId,
        ReminderSourceType sourceType,
        Boolean requiresResponse
) {
}




