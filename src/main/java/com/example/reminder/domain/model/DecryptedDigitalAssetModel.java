package com.example.reminder.domain.model;

import java.time.LocalDateTime;

public record DecryptedDigitalAssetModel(
        Long assetId,
        String secret,
        LocalDateTime decryptedAt
) {
}
