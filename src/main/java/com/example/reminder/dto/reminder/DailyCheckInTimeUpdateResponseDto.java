package com.example.reminder.dto.reminder;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record DailyCheckInTimeUpdateResponseDto(
        LocalTime dailyCheckInTime,
        LocalDateTime nextRegularTime,
        LocalDateTime transitionTime,
        LocalDateTime expectedMissedAt,
        boolean nightRisk,
        String warningMessage
) {
}
