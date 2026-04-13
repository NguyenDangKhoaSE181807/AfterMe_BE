package com.example.reminder.service.impl;

import com.example.reminder.domain.model.DigitalAssetModel;
import com.example.reminder.dto.digitalasset.CreateDigitalAssetCommand;
import com.example.reminder.entity.AssetShare;
import com.example.reminder.entity.DigitalAsset;
import com.example.reminder.entity.DigitalAssetVersion;
import com.example.reminder.entity.User;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.AssetShareRepository;
import com.example.reminder.repository.DigitalAssetRepository;
import com.example.reminder.repository.DigitalAssetVersionRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.DigitalAssetService;
import com.example.reminder.service.security.EncryptionResult;
import com.example.reminder.service.security.SecretEncryptionService;
import java.time.LocalDateTime;
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
    private final AssetShareRepository assetShareRepository;
    private final UserRepository userRepository;
    private final SecretEncryptionService secretEncryptionService;

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
