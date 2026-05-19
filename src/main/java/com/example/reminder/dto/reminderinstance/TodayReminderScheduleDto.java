package com.example.reminder.dto.reminderinstance;

import com.example.reminder.domain.enums.DayOfWeek;
import com.example.reminder.domain.enums.ReminderInstanceStatus;
import com.example.reminder.domain.enums.ScheduleType;
import java.time.LocalDateTime;
import java.util.Set;

public record TodayReminderScheduleDto(
        Long instanceId,
        Long reminderId,
        Long scheduleId,
        String reminderTitle,
        String reminderDescription,
        ScheduleType scheduleType,
        Set<DayOfWeek> daysOfWeek,
        LocalDateTime scheduledTime,
        ReminderInstanceStatus status,
        Integer escalationLevel,
        Integer missedCount
) {
}