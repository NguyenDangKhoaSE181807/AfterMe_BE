package com.example.reminder.application.dto.habit;

import com.example.reminder.domain.enums.HabitCategory;

public record CreateHabitCommand(
        Long userId,
        String name,
        HabitCategory category
) {
}




