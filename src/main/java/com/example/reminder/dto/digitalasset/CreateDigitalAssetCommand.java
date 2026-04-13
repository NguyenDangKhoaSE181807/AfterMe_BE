package com.example.reminder.dto.digitalasset;

public record CreateDigitalAssetCommand(
        Long userId,
        String name,
        String type,
        String identifier,
        String secret,
        String instructions
) {
}
