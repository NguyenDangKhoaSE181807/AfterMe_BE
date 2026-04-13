package com.example.reminder.domain.model;

import java.time.LocalDateTime;

public record DigitalAssetModel(
        Long id,
        Long userId,
        String name,
        String type,
        String identifier,
        String identifierType,
        String identifierValue,
        String accessInstructions,
        Boolean isActive,
        LocalDateTime createdAt
) {
}
