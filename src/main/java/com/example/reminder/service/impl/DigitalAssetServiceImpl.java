package com.example.reminder.service.impl;

import com.example.reminder.entity.AssetShare;
import com.example.reminder.entity.DigitalAsset;
import com.example.reminder.entity.DigitalAssetVersion;
import com.example.reminder.repository.AssetShareRepository;
import com.example.reminder.repository.DigitalAssetRepository;
import com.example.reminder.repository.DigitalAssetVersionRepository;
import com.example.reminder.service.DigitalAssetService;
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
}
