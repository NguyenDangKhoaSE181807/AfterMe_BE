package com.example.reminder.dto.reminder;

import jakarta.validation.constraints.NotNull;

public record SkipDailyCheckInTransitionRequest(
        @NotNull Long transitionInstanceId
) {
}
