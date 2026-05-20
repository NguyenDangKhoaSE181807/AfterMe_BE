package com.example.reminder.dto.digitalasset;

import java.time.LocalDateTime;

public record UpdateDigitalAssetSecretResponseDto(
        String message,
        LocalDateTime updatedAt
) {
}
