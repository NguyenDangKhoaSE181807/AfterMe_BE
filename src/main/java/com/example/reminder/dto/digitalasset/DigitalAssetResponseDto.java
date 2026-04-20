package com.example.reminder.dto.digitalasset;

import java.time.LocalDateTime;

public record DigitalAssetResponseDto(
        Long id,
        Long userId,
        String name,
        String type,
        String identifier,
        String identifierType,
        String identifierValue,
        String instructions,
        Boolean isActive,
        LocalDateTime createdAt
) {
}
