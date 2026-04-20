package com.example.reminder.dto.digitalasset;

public record ConsumeSecretTokenCommand(
        String token,
        String actorId,
        String ipAddress,
        String requestId,
        String userAgent,
        String requestPath,
        String httpMethod
) {
}
