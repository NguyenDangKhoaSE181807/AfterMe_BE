package com.example.reminder.dto.reminderinstance;

import com.example.reminder.domain.enums.UserResponseAction;
import jakarta.validation.constraints.NotNull;

public record UserResponseRequest(
        @NotNull(message = "ID lần nhắc là bắt buộc")
        Long instanceId,
        @NotNull(message = "Hành động phản hồi là bắt buộc")
        UserResponseAction action
) {
}
