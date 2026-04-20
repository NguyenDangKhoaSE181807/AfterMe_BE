package com.example.reminder.domain.model;

import java.time.LocalDateTime;

public record DecryptTokenModel(
        Long assetId,
        String oneTimeToken,
        LocalDateTime expiresAt
) {
}
