package com.example.reminder.dto.reminderinstance;

import com.example.reminder.domain.enums.UserResponseAction;
import jakarta.validation.constraints.NotNull;

public record UserResponseRequest(
        @NotNull Long instanceId,
        @NotNull UserResponseAction action
) {
}
