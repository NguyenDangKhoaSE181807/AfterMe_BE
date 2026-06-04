package com.example.reminder.dto.userdevice;

import jakarta.validation.constraints.NotNull;

public record UpdateUserDeviceNotificationRequest(
        @NotNull Boolean notificationEnabled
) {
}