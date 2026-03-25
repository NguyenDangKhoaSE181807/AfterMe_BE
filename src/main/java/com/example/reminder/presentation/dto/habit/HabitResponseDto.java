package com.example.reminder.presentation.dto.habit;

import com.example.reminder.domain.enums.HabitCategory;
import java.time.LocalDateTime;

public record HabitResponseDto(
        Long id,
        Long userId,
        String name,
        HabitCategory category,
        LocalDateTime createdAt
) {
}





