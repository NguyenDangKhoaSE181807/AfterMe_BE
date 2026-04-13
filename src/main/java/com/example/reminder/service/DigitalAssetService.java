package com.example.reminder.service;

import com.example.reminder.entity.AssetShare;
import com.example.reminder.entity.DigitalAsset;
import java.util.List;
import java.util.Optional;

public interface DigitalAssetService {

    List<DigitalAsset> findByUserId(Long userId);

    Optional<DigitalAsset> findById(Long id);

    DigitalAsset save(DigitalAsset asset);

    boolean canDecryptAssetShare(AssetShare assetShare);

    void softDeleteAsset(Long assetId);
}
