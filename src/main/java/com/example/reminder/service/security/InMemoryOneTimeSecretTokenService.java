package com.example.reminder.service.security;

import com.example.reminder.entity.DigitalAssetSecretToken;
import com.example.reminder.exception.DecryptDenyReason;
import com.example.reminder.exception.DecryptDeniedException;
import com.example.reminder.repository.DigitalAssetRepository;
import com.example.reminder.repository.DigitalAssetSecretTokenRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InMemoryOneTimeSecretTokenService implements OneTimeSecretTokenService {

    private final DigitalAssetSecretTokenRepository tokenRepository;
    private final DigitalAssetRepository digitalAssetRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int ttlSeconds;

    public InMemoryOneTimeSecretTokenService(
            DigitalAssetSecretTokenRepository tokenRepository,
            DigitalAssetRepository digitalAssetRepository,
            @Value("${app.security.secret-token.ttl-seconds:60}") int ttlSeconds
    ) {
        this.tokenRepository = tokenRepository;
        this.digitalAssetRepository = digitalAssetRepository;
        this.ttlSeconds = Math.max(10, ttlSeconds);
    }

    @Override
    @Transactional
    public IssuedSecretToken issueToken(Long assetId, String actorId, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(ttlSeconds);
        String token = generateToken();
        String tokenHash = TokenHashingUtil.sha256Hex(token);

        DigitalAssetSecretToken secretToken = new DigitalAssetSecretToken();
        secretToken.setDigitalAsset(digitalAssetRepository.getReferenceById(assetId));
        secretToken.setTokenHash(tokenHash);
        secretToken.setActorId(actorId);
        secretToken.setIpAddress(ipAddress);
        secretToken.setExpiresAt(expiresAt);
        secretToken.setCreatedAt(now);
        tokenRepository.save(secretToken);
        return new IssuedSecretToken(token, expiresAt);
    }

    @Override
    @Transactional
    public ConsumedSecretToken consumeToken(String token, String actorId, String ipAddress) {
        if (token == null || token.isBlank()) {
            throw new DecryptDeniedException(DecryptDenyReason.TOKEN_INVALID, "Token is required");
        }

        LocalDateTime now = LocalDateTime.now();
        String tokenHash = TokenHashingUtil.sha256Hex(token);
        int updated = tokenRepository.markConsumedIfValid(tokenHash, actorId, ipAddress, now);
        Optional<DigitalAssetSecretToken> entryOptional = tokenRepository.findByTokenHash(tokenHash);
        if (updated == 0) {
            DigitalAssetSecretToken entry = entryOptional.orElseThrow(() ->
                    new DecryptDeniedException(DecryptDenyReason.TOKEN_INVALID, "Token is invalid or expired")
            );
            if (entry.getConsumedAt() != null || entry.getExpiresAt().isBefore(now)) {
                throw new DecryptDeniedException(DecryptDenyReason.TOKEN_INVALID, "Token is invalid or expired");
            }
            if (!entry.getActorId().equals(actorId) || !entry.getIpAddress().equals(ipAddress)) {
                throw new DecryptDeniedException(
                        DecryptDenyReason.TOKEN_CONTEXT_MISMATCH,
                        "Token context mismatch"
                );
            }
            throw new DecryptDeniedException(DecryptDenyReason.TOKEN_INVALID, "Token is invalid or expired");
        }

        DigitalAssetSecretToken entry = entryOptional.orElseThrow(() ->
                new DecryptDeniedException(DecryptDenyReason.TOKEN_INVALID, "Token is invalid or expired")
        );

        return new ConsumedSecretToken(
                entry.getDigitalAsset().getId(),
                entry.getActorId(),
                entry.getIpAddress()
        );
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
