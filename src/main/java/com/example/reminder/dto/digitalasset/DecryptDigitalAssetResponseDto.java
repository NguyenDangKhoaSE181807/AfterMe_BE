package com.example.reminder.dto.digitalasset;

import java.time.LocalDateTime;

public record DecryptDigitalAssetResponseDto(
        Long assetId,
        String oneTimeToken,
        LocalDateTime expiresAt
) {
}
