package com.example.reminder.service.security;

import java.time.LocalDateTime;

public record IssuedSecretToken(
        String token,
        LocalDateTime expiresAt
) {
}
