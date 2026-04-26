package com.example.reminder.dto.reminder;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.ScheduleType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record AdminReminderMetadataDto(
        Long reminderId,
        ReminderStatus status,
        int scheduleCount,
        Set<ScheduleType> frequencyTypes,
        List<Integer> intervalValues,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime archivedAt
) {
}
