package com.example.reminder.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ValidationErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {
}
