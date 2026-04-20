package com.example.reminder.service.security;

public record ConsumedSecretToken(
        Long assetId,
        String actorId,
        String ipAddress
) {
}
