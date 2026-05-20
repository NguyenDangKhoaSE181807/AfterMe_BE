package com.example.reminder.dto.digitalasset;

import java.time.LocalDateTime;

public record DigitalAssetListResponseDto(
        Long id,
        String name,
        String type,
        String identifier,
        String instructions,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
