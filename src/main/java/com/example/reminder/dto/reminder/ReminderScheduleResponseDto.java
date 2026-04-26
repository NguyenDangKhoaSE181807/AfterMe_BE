package com.example.reminder.dto.reminder;

import com.example.reminder.domain.enums.DayOfWeek;
import com.example.reminder.domain.enums.ScheduleType;
import java.time.LocalDateTime;
import java.util.Set;

public record ReminderScheduleResponseDto(
        Long id,
        Long reminderId,
        ScheduleType type,
        Integer intervalValue,
        Set<DayOfWeek> daysOfWeek,
        LocalDateTime startDatetime,
        LocalDateTime endDatetime
) {
}
