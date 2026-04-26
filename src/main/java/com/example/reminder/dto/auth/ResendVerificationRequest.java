package com.example.reminder.dto.auth;

import jakarta.validation.constraints.NotNull;

public record ResendVerificationRequest(
        @NotNull(message = "User ID is required")
        Long userId
) {
}
