package com.example.reminder.dto.digitalasset;

import java.time.LocalDateTime;

public record OwnerDecryptResponseDto(
        Long assetId,
        String secret,
        LocalDateTime decryptedAt
) {
}
