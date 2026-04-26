package com.example.reminder.dto.reminder;

import com.example.reminder.domain.enums.ReminderStatus;
import java.util.List;
import java.util.Map;

public record AdminReminderOverviewDto(
        Long targetUserId,
        int totalReminders,
        Map<ReminderStatus, Long> statusCounts,
        List<AdminReminderMetadataDto> reminders
) {
}
