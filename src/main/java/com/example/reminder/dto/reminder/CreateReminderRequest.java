package com.example.reminder.dto.reminder;

import com.example.reminder.domain.enums.TonePreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReminderRequest(
        Long habitId,
        @NotBlank(message = "Tiêu đề lời nhắc là bắt buộc")
        @Size(max = 255, message = "Tiêu đề lời nhắc không được vượt quá 255 ký tự")
        String title,
        String description,
        TonePreference tone,
        Boolean safetyEnabled
) {
}





