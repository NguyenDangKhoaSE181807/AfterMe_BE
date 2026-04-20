package com.example.reminder.service.security;

import com.example.reminder.exception.DecryptDenyReason;
import com.example.reminder.exception.DecryptDeniedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InMemoryOneTimeSecretTokenService implements OneTimeSecretTokenService {

    private final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();
    private final int ttlSeconds;

    public InMemoryOneTimeSecretTokenService(
            @Value("${app.security.secret-token.ttl-seconds:60}") int ttlSeconds
    ) {
        this.ttlSeconds = Math.max(10, ttlSeconds);
    }

    @Override
    public IssuedSecretToken issueToken(Long assetId, String actorId, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(ttlSeconds);
        String token = UUID.randomUUID().toString();
        String tokenHash = hashToken(token);

        purgeExpired(now);
        tokenStore.put(tokenHash, new TokenEntry(assetId, actorId, ipAddress, expiresAt));
        return new IssuedSecretToken(token, expiresAt);
    }

    @Override
    public ConsumedSecretToken consumeToken(String token, String actorId, String ipAddress) {
        if (token == null || token.isBlank()) {
            throw new DecryptDeniedException(DecryptDenyReason.TOKEN_INVALID, "Token is required");
        }

        LocalDateTime now = LocalDateTime.now();
        TokenEntry entry = tokenStore.remove(hashToken(token));
        if (entry == null || entry.expiresAt().isBefore(now)) {
            throw new DecryptDeniedException(DecryptDenyReason.TOKEN_INVALID, "Token is invalid or expired");
        }

        if (!entry.actorId().equals(actorId) || !entry.ipAddress().equals(ipAddress)) {
            throw new DecryptDeniedException(
                    DecryptDenyReason.TOKEN_CONTEXT_MISMATCH,
                    "Token context mismatch"
            );
        }

        return new ConsumedSecretToken(entry.assetId(), entry.actorId(), entry.ipAddress());
    }

    private void purgeExpired(LocalDateTime now) {
        tokenStore.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }

    private record TokenEntry(Long assetId, String actorId, String ipAddress, LocalDateTime expiresAt) {
    }
}
