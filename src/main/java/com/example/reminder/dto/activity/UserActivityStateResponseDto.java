package com.example.reminder.dto.activity;

import com.example.reminder.domain.enums.UserActivitySignalType;
import java.time.LocalDateTime;

public record UserActivityStateResponseDto(
        String deviceId,
        LocalDateTime lastAppForegroundAt,
        LocalDateTime lastAppInteractionAt,
        LocalDateTime lastPushTappedAt,
        LocalDateTime lastDeviceUnlockedAt,
        LocalDateTime lastDeviceInteractiveAt,
        LocalDateTime lastMotionAt,
        UserActivitySignalType lastActivityType,
        LocalDateTime updatedAt
) {
}
