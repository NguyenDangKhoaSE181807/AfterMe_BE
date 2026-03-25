package com.example.reminder.domain.model;

import com.example.reminder.domain.enums.HabitCategory;
import java.time.LocalDateTime;

public record HabitModel(
        Long id,
        Long userId,
        String name,
        HabitCategory category,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
}




