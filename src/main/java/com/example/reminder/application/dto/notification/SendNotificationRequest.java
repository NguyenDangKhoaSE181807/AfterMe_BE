package com.example.reminder.application.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendNotificationRequest(
        @NotNull Long userId,
        @NotBlank String title,
        @NotBlank String body
) {
}




