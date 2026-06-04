package com.example.reminder.dto.userdevice;

import jakarta.validation.constraints.NotBlank;

public record UpsertUserDeviceRequest(
        @NotBlank String fcmToken,
        String platform,
        Boolean notificationEnabled,
        Boolean isTrusted
) {
}