package com.example.reminder.dto.activity;

import com.example.reminder.domain.enums.UserActivitySignalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record UserActivitySignalRequest(
        @NotBlank String deviceId,
        @NotNull UserActivitySignalType signalType,
        @NotNull LocalDateTime occurredAt,
        String source,
        Double confidence
) {
}
