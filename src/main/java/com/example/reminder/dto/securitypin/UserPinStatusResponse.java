package com.example.reminder.dto.securitypin;

import java.time.LocalDateTime;

public record UserPinStatusResponse(
        boolean configured,
        Integer remainingAttempts,
        LocalDateTime lockedUntil
) {
}
