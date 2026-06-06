package com.example.reminder.dto.reminder;

import com.example.reminder.domain.enums.DayOfWeek;
import com.example.reminder.domain.enums.ScheduleType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;

public record UpdateReminderScheduleRequest(
        @NotNull(message = "Loại lịch nhắc là bắt buộc")
        ScheduleType type,
        Integer intervalValue,
        Set<DayOfWeek> daysOfWeek,
        @NotNull(message = "Thời gian bắt đầu là bắt buộc")
        LocalDateTime startDatetime,
        LocalDateTime endDatetime
) {
}
