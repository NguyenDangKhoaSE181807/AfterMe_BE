package com.example.reminder.dto.reminderinstance;

import com.example.reminder.domain.enums.UserResponseAction;
import jakarta.validation.constraints.NotNull;

public record CreateUserResponseRequest(
        @NotNull(message = "action is required")
        UserResponseAction action,
        String payload
) {
}
