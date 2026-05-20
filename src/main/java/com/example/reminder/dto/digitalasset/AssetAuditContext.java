package com.example.reminder.dto.digitalasset;

public record AssetAuditContext(
        String actorId,
        String ipAddress,
        String requestId,
        String userAgent,
        String requestPath,
        String httpMethod
) {
}
