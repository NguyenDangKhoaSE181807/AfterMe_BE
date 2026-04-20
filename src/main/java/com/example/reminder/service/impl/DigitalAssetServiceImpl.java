package com.example.reminder.service.impl;

import com.example.reminder.domain.model.DigitalAssetModel;
import com.example.reminder.domain.model.DecryptTokenModel;
import com.example.reminder.domain.model.DecryptedDigitalAssetModel;
import com.example.reminder.dto.digitalasset.CreateDigitalAssetCommand;
import com.example.reminder.dto.digitalasset.ConsumeSecretTokenCommand;
import com.example.reminder.dto.digitalasset.DecryptDigitalAssetCommand;
import com.example.reminder.entity.AssetAccessLog;
import com.example.reminder.entity.AssetAccessForensicLog;
import com.example.reminder.entity.AssetShare;
import com.example.reminder.entity.DigitalAsset;
import com.example.reminder.entity.DigitalAssetVersion;
import com.example.reminder.entity.User;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.DecryptDenyReason;
import com.example.reminder.exception.DecryptDeniedException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.exception.TooManyRequestsException;
import com.example.reminder.repository.AssetAccessLogRepository;
import com.example.reminder.repository.AssetAccessForensicLogRepository;
import com.example.reminder.repository.AssetShareRepository;
import com.example.reminder.repository.DigitalAssetRepository;
import com.example.reminder.repository.DigitalAssetVersionRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.DigitalAssetService;
import com.example.reminder.service.security.ConsumedSecretToken;
import com.example.reminder.service.security.EncryptionResult;
import com.example.reminder.service.security.IssuedSecretToken;
import com.example.reminder.service.security.OneTimeSecretTokenService;
import com.example.reminder.service.security.RequestRateLimiter;
import com.example.reminder.service.security.SecretEncryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DigitalAssetServiceImpl implements DigitalAssetService {

    private final DigitalAssetRepository digitalAssetRepository;
    private final DigitalAssetVersionRepository digitalAssetVersionRepository;
    private final AssetAccessLogRepository assetAccessLogRepository;
    private final AssetAccessForensicLogRepository assetAccessForensicLogRepository;
    private final AssetShareRepository assetShareRepository;
    private final UserRepository userRepository;
    private final SecretEncryptionService secretEncryptionService;
    private final OneTimeSecretTokenService oneTimeSecretTokenService;
    private final RequestRateLimiter requestRateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    public List<DigitalAsset> findByUserId(Long userId) {
        return digitalAssetRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public Optional<DigitalAsset> findById(Long id) {
        return digitalAssetRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    @Transactional
    public DigitalAsset save(DigitalAsset asset) {
        if (asset.getIdentifierValue() == null || asset.getIdentifierValue().isBlank()) {
            asset.setIdentifierValue(asset.getIdentifier());
        }

        DigitalAsset savedAsset = digitalAssetRepository.save(asset);
        persistAssetVersion(savedAsset);
        return savedAsset;
    }

    @Override
    @Transactional
    public DigitalAssetModel create(CreateDigitalAssetCommand command) {
        User user = userRepository.findByIdAndDeletedAtIsNull(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + command.userId()));

        String encryptionKeyId = secretEncryptionService.generateEncryptionKeyId();
        EncryptionResult encryptionResult = secretEncryptionService.encrypt(command.secret(), encryptionKeyId);

        DigitalAsset asset = new DigitalAsset();
        asset.setUser(user);
        asset.setName(command.name());
        asset.setType(command.type());
        asset.setIdentifier(command.identifier());
        asset.setIdentifierType(inferIdentifierType(command.identifier()));
        asset.setIdentifierValue(command.identifier());
        asset.setEncryptedSecret(encryptionResult.cipherText());
        asset.setEncryptionIv(encryptionResult.iv());
        asset.setEncryptionAlgo(encryptionResult.algorithm());
        asset.setEncryptionKeyId(encryptionKeyId);
        asset.setAccessInstructions(command.instructions());
        asset.setIsActive(true);
        asset.setCreatedAt(LocalDateTime.now());

        DigitalAsset saved = save(asset);
        return toModel(saved);
    }

    @Override
    @Transactional
    public DecryptTokenModel decrypt(DecryptDigitalAssetCommand command) {
        requestRateLimiter.checkRateLimit("DECRYPT", command.actorId(), command.ipAddress());

        Optional<DigitalAsset> assetOptional = digitalAssetRepository.findByIdAndDeletedAtIsNull(command.assetId());
        if (assetOptional.isEmpty()) {
            logForensicDenied(command.assetId(), command, DecryptDenyReason.ASSET_NOT_FOUND);
            throw new ResourceNotFoundException("Digital asset not found: " + command.assetId());
        }

        DigitalAsset asset = assetOptional.get();

        try {
            AssetShare share = assetShareRepository
                    .findByDigitalAssetIdAndTrustedContactIdAndDeletedAtIsNull(asset.getId(), command.trustedContactId())
                    .orElseThrow(() -> new DecryptDeniedException(
                            DecryptDenyReason.CONTACT_ACCESS_MISMATCH,
                            "Trusted contact does not have access to this asset"
                    ));

            validateActorBinding(command.actorId(), command.trustedContactId());

            if (!Boolean.TRUE.equals(share.getTrustedContact().getIsActive()) || share.getTrustedContact().getDeletedAt() != null) {
                throw new DecryptDeniedException(DecryptDenyReason.CONTACT_INACTIVE, "Trusted contact is inactive");
            }

            enforceDecryptPolicy(asset, share);

            IssuedSecretToken issuedToken = oneTimeSecretTokenService.issueToken(
                    asset.getId(),
                    command.actorId(),
                    command.ipAddress()
            );
            logAssetAccess(asset, "DECRYPT_TOKEN_ISSUED", null, command);
            return new DecryptTokenModel(asset.getId(), issuedToken.token(), issuedToken.expiresAt());
        } catch (RuntimeException ex) {
            logAssetAccess(asset, "DECRYPT_DENIED", resolveDenyReason(ex), command);
            throw ex;
        }
    }

    @Override
    @Transactional
    public DecryptedDigitalAssetModel consumeSecretToken(ConsumeSecretTokenCommand command) {
        requestRateLimiter.checkRateLimit("CONSUME_SECRET_TOKEN", command.actorId(), command.ipAddress());

        final ConsumedSecretToken consumed;
        try {
            consumed = oneTimeSecretTokenService.consumeToken(command.token(), command.actorId(), command.ipAddress());
        } catch (RuntimeException ex) {
            logForensicDenied(null, command, resolveDenyReason(ex));
            throw ex;
        }

        Optional<DigitalAsset> assetOptional = digitalAssetRepository.findByIdAndDeletedAtIsNull(consumed.assetId());
        if (assetOptional.isEmpty()) {
            logForensicDenied(consumed.assetId(), command, DecryptDenyReason.ASSET_NOT_FOUND);
            throw new ResourceNotFoundException("Digital asset not found: " + consumed.assetId());
        }

        DigitalAsset asset = assetOptional.get();

        String secret = secretEncryptionService.decrypt(
                asset.getEncryptedSecret(),
                asset.getEncryptionIv(),
                asset.getEncryptionKeyId(),
                asset.getEncryptionAlgo()
        );

        logAssetAccess(asset, "DECRYPT", null, command);
        return new DecryptedDigitalAssetModel(asset.getId(), secret, LocalDateTime.now());
    }

    @Override
    public boolean canDecryptAssetShare(AssetShare assetShare) {
        if (assetShare == null || assetShare.getDeletedAt() != null) {
            return false;
        }

        return "UNLOCKED".equalsIgnoreCase(assetShare.getUnlockStatus())
            && Boolean.TRUE.equals(assetShare.getIsUnlocked())
            && assetShare.getUnlockedAt() != null;
    }

    @Override
    @Transactional
    public void softDeleteAsset(Long assetId) {
        Optional<DigitalAsset> assetOptional = digitalAssetRepository.findByIdAndDeletedAtIsNull(assetId);
        if (assetOptional.isEmpty()) {
            return;
        }

        DigitalAsset asset = assetOptional.get();
        LocalDateTime now = LocalDateTime.now();
        asset.setDeletedAt(now);
        asset.setIsActive(false);
        digitalAssetRepository.save(asset);
        assetShareRepository.revokeAllActiveSharesByDigitalAssetId(assetId);
    }

    private void persistAssetVersion(DigitalAsset asset) {
        if (asset.getEncryptedSecret() == null || asset.getEncryptedSecret().isBlank()) {
            return;
        }

        DigitalAssetVersion version = new DigitalAssetVersion();
        version.setAsset(asset);
        version.setEncryptedSecret(asset.getEncryptedSecret());
        version.setEncryptionIv(asset.getEncryptionIv());
        version.setEncryptionAlgo(asset.getEncryptionAlgo());
        version.setEncryptionKeyId(asset.getEncryptionKeyId());
        version.setVersion(asset.getVersion());
        version.setCreatedAt(LocalDateTime.now());
        digitalAssetVersionRepository.save(version);
    }

    private void enforceDecryptPolicy(DigitalAsset asset, AssetShare share) {
        if (!canDecryptAssetShare(share)) {
            throw new DecryptDeniedException(DecryptDenyReason.NOT_UNLOCKED, "Asset share is not unlocked");
        }

        LocalDateTime unlockAllowedAt = share.getUnlockedAt();
        int effectiveDelayHours = share.getUnlockDelayHours() == null ? 0 : share.getUnlockDelayHours();

        if (share.getUnlockPolicy() != null && !share.getUnlockPolicy().isBlank()) {
            try {
                JsonNode policy = objectMapper.readTree(share.getUnlockPolicy());

                if (policy.path("delay_hours").canConvertToInt()) {
                    effectiveDelayHours = Math.max(effectiveDelayHours, policy.path("delay_hours").asInt());
                }

                if (policy.path("require_user_inactive").asBoolean(false)
                        && asset.getUser().getStatus() != com.example.reminder.domain.enums.UserStatus.SUSPENDED) {
                    throw new DecryptDeniedException(
                            DecryptDenyReason.POLICY_USER_INACTIVE,
                            "Policy requires user to be inactive before decrypt"
                    );
                }

                if (policy.path("require_multiple_contacts").canConvertToInt()) {
                    int required = policy.path("require_multiple_contacts").asInt();
                    long unlockedCount = assetShareRepository
                            .countByDigitalAssetIdAndUnlockStatusAndDeletedAtIsNull(asset.getId(), "UNLOCKED");

                    if (required > 0 && unlockedCount < required) {
                        throw new DecryptDeniedException(
                                DecryptDenyReason.POLICY_MULTIPLE_CONTACTS,
                                "Policy requires multiple unlocked trusted contacts"
                        );
                    }
                }
            } catch (BadRequestException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new DecryptDeniedException(
                        DecryptDenyReason.INVALID_POLICY,
                        "Invalid unlock policy configuration"
                );
            }
        }

        if (effectiveDelayHours > 0) {
            unlockAllowedAt = unlockAllowedAt.plus(effectiveDelayHours, ChronoUnit.HOURS);
        }

        if (unlockAllowedAt.isAfter(LocalDateTime.now())) {
            throw new DecryptDeniedException(DecryptDenyReason.DELAY_NOT_ELAPSED, "Unlock delay has not elapsed yet");
        }
    }

    private void validateActorBinding(String actorId, Long trustedContactId) {
        if (actorId == null || actorId.isBlank()) {
            throw new DecryptDeniedException(DecryptDenyReason.ACTOR_MISMATCH, "Missing actor identity");
        }

        String expected = "trusted-contact:" + trustedContactId;
        if (!expected.equals(actorId) && !trustedContactId.toString().equals(actorId)) {
            throw new DecryptDeniedException(
                    DecryptDenyReason.ACTOR_MISMATCH,
                    "Actor does not match trusted contact"
            );
        }
    }

    private void logAssetAccess(
            DigitalAsset asset,
            String action,
            DecryptDenyReason reason,
            DecryptDigitalAssetCommand command
    ) {
        AssetAccessLog log = new AssetAccessLog();
        log.setDigitalAsset(asset);
        log.setAccessedBy(command.actorId());
        log.setAction(action);
        log.setIpAddress(command.ipAddress());
        log.setReasonCode(reason == null ? null : reason.name());
        log.setRequestId(command.requestId());
        log.setUserAgent(command.userAgent());
        log.setRequestPath(command.requestPath());
        log.setHttpMethod(command.httpMethod());
        log.setCreatedAt(LocalDateTime.now());
        assetAccessLogRepository.save(log);
    }

    private void logAssetAccess(
            DigitalAsset asset,
            String action,
            DecryptDenyReason reason,
            ConsumeSecretTokenCommand command
    ) {
        AssetAccessLog log = new AssetAccessLog();
        log.setDigitalAsset(asset);
        log.setAccessedBy(command.actorId());
        log.setAction(action);
        log.setIpAddress(command.ipAddress());
        log.setReasonCode(reason == null ? null : reason.name());
        log.setRequestId(command.requestId());
        log.setUserAgent(command.userAgent());
        log.setRequestPath(command.requestPath());
        log.setHttpMethod(command.httpMethod());
        log.setCreatedAt(LocalDateTime.now());
        assetAccessLogRepository.save(log);
    }

    private void logForensicDenied(Long attemptedAssetId, DecryptDigitalAssetCommand command, DecryptDenyReason reason) {
        AssetAccessForensicLog log = new AssetAccessForensicLog();
        log.setAttemptedAssetId(attemptedAssetId);
        log.setActorId(command.actorId());
        log.setAction("DECRYPT_DENIED");
        log.setReasonCode(reason.name());
        log.setIpAddress(command.ipAddress());
        log.setRequestId(command.requestId());
        log.setUserAgent(command.userAgent());
        log.setRequestPath(command.requestPath());
        log.setHttpMethod(command.httpMethod());
        log.setCreatedAt(LocalDateTime.now());
        assetAccessForensicLogRepository.save(log);
    }

    private void logForensicDenied(Long attemptedAssetId, ConsumeSecretTokenCommand command, DecryptDenyReason reason) {
        AssetAccessForensicLog log = new AssetAccessForensicLog();
        log.setAttemptedAssetId(attemptedAssetId);
        log.setActorId(command.actorId());
        log.setAction("CONSUME_DENIED");
        log.setReasonCode(reason.name());
        log.setIpAddress(command.ipAddress());
        log.setRequestId(command.requestId());
        log.setUserAgent(command.userAgent());
        log.setRequestPath(command.requestPath());
        log.setHttpMethod(command.httpMethod());
        log.setCreatedAt(LocalDateTime.now());
        assetAccessForensicLogRepository.save(log);
    }

    private DecryptDenyReason resolveDenyReason(RuntimeException ex) {
        if (ex instanceof DecryptDeniedException denied) {
            return denied.getReason();
        }

        if (ex instanceof TooManyRequestsException) {
            return DecryptDenyReason.RATE_LIMITED;
        }

        return DecryptDenyReason.UNKNOWN;
    }

    private String inferIdentifierType(String identifier) {
        if (identifier != null && identifier.contains("@")) {
            return "EMAIL";
        }

        return "USERNAME";
    }

    private DigitalAssetModel toModel(DigitalAsset asset) {
        return new DigitalAssetModel(
                asset.getId(),
                asset.getUser().getId(),
                asset.getName(),
                asset.getType(),
                asset.getIdentifier(),
                asset.getIdentifierType(),
                asset.getIdentifierValue(),
                asset.getAccessInstructions(),
                asset.getIsActive(),
                asset.getCreatedAt()
        );
    }
}
