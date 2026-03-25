package com.example.reminder.presentation.exception;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp
) {
}





