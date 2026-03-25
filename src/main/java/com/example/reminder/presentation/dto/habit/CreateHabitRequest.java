package com.example.reminder.presentation.dto.habit;

import com.example.reminder.domain.enums.HabitCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateHabitRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 255) String name,
        @NotNull HabitCategory category
) {
}





