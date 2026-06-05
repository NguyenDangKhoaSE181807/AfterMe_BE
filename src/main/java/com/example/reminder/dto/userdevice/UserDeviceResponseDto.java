package com.example.reminder.dto.userdevice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserDeviceResponseDto(
        Long id,
        String deviceId,
        String fcmToken,
        String platform,
        Boolean isTrusted,
        Boolean notificationEnabled,
        LocalDateTime lastSeenAt,
        BigDecimal lastLatitude,
        BigDecimal lastLongitude,
        Integer lastLocationAccuracyMeters,
        LocalDateTime lastLocationAt,
        String lastLocationSource
) {
}
