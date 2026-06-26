package com.example.reminder.dto.activity;

import jakarta.validation.constraints.NotNull;

public record PassiveActivitySettingsRequest(
        @NotNull Boolean passiveActivityAssistEnabled
) {
}
