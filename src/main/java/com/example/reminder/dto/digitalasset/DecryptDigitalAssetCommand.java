package com.example.reminder.dto.digitalasset;

public record DecryptDigitalAssetCommand(
        Long assetId,
        Long trustedContactId,
        String actorId,
        String ipAddress,
        String requestId,
        String userAgent,
        String requestPath,
        String httpMethod
) {
}
