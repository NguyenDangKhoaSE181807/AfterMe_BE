package com.example.reminder.domain.model;

import com.example.reminder.domain.enums.ReminderSourceType;
import java.time.LocalDateTime;

public record NotificationMessage(
        Long userId,
        String title,
        String body,
        LocalDateTime sentAt,
        Long reminderId,
        Long scheduleId,
        Long instanceId,
        ReminderSourceType sourceType,
        Boolean requiresResponse
) {
}




