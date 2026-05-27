package com.example.reminder.dto.userdevice;

import java.time.LocalDateTime;

public record UserDeviceResponseDto(
        Long id,
        String deviceId,
        String fcmToken,
        String platform,
        Boolean isTrusted,
        Boolean notificationEnabled,
        LocalDateTime lastSeenAt
) {
}