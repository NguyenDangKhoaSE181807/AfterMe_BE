package com.example.reminder.dto.habit;

import com.example.reminder.domain.enums.HabitCategory;

public record UpdateHabitCommand(
        Long userId,
        String name,
        HabitCategory category
) {
}
