package com.example.reminder.dto.digitalasset;

import java.time.LocalDateTime;

public record ConsumeSecretTokenResponseDto(
        Long assetId,
        String secret,
        LocalDateTime consumedAt
) {
}
