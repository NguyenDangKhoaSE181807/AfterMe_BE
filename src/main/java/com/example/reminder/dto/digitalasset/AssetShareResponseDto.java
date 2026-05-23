package com.example.reminder.dto.digitalasset;

import java.time.LocalDateTime;

public record AssetShareResponseDto(
        Long id,
        Long assetId,
        Long trustedContactId,
        String trustedContactName,
        String trustedContactRelationship,
        Boolean isUnlocked,
        String unlockStatus,
        String unlockCondition,
        Integer unlockDelayHours,
        String unlockPolicy,
        LocalDateTime unlockedAt,
        LocalDateTime createdAt
) {
}
