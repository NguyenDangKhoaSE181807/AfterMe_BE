package com.example.reminder.dto.activity;

import com.example.reminder.domain.enums.ActivityLogType;
import java.time.LocalDateTime;

public record ActivityLogResponseDto(
        Long id,
        Long userId,
        ActivityLogType type,
        String title,
        String message,
        Long reminderId,
        Long scheduleId,
        Long instanceId,
        String metadata,
        LocalDateTime createdAt
) {
}
