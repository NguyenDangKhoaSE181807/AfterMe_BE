package com.example.reminder.dto.reminder;

import com.example.reminder.domain.enums.DayOfWeek;
import com.example.reminder.domain.enums.ScheduleType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;

public record CreateReminderScheduleRequest(
        @NotNull ScheduleType type,
        Integer intervalValue,
        Set<DayOfWeek> daysOfWeek,
        @NotNull LocalDateTime startDatetime,
        LocalDateTime endDatetime
) {
}
