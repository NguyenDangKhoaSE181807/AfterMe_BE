package com.example.reminder.service.security;

public interface OneTimeSecretTokenService {

    IssuedSecretToken issueToken(Long assetId, String actorId, String ipAddress);

    ConsumedSecretToken consumeToken(String token, String actorId, String ipAddress);
}
