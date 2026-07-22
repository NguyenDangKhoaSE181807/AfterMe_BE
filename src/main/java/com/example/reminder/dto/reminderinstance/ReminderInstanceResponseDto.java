package com.example.reminder.dto.reminderinstance;

import com.example.reminder.domain.enums.ReminderInstanceStatus;
import java.time.LocalDateTime;

public record ReminderInstanceResponseDto(
        Long id,
        Long reminderId,
        Long scheduleId,
        LocalDateTime scheduledTime,
        ReminderInstanceStatus status,
        Integer escalationLevel,
        Integer missedCount,
        LocalDateTime lastNotificationAt,
        LocalDateTime resolvedAt,
        LocalDateTime deletedAt,
        LocalDateTime earlyCheckInStartAt,
        LocalDateTime smartCheckInWindowEndAt,
        LocalDateTime responseDeadline,
        Boolean canCheckInNow,
        String checkInPromptReason
) {
}
