package com.example.reminder.domain.model;

import java.time.LocalDateTime;

public record NotificationMessage(
        Long userId,
        String title,
        String body,
        LocalDateTime sentAt
) {
}




