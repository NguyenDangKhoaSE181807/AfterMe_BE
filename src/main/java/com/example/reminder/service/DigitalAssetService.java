package com.example.reminder.service;

import com.example.reminder.domain.model.DigitalAssetModel;
import com.example.reminder.domain.model.DecryptTokenModel;
import com.example.reminder.domain.model.DecryptedDigitalAssetModel;
import com.example.reminder.dto.digitalasset.CreateDigitalAssetCommand;
import com.example.reminder.dto.digitalasset.ConsumeSecretTokenCommand;
import com.example.reminder.dto.digitalasset.DecryptDigitalAssetCommand;
import com.example.reminder.entity.AssetShare;
import com.example.reminder.entity.DigitalAsset;
import java.util.List;
import java.util.Optional;

public interface DigitalAssetService {

    List<DigitalAsset> findByUserId(Long userId);

    Optional<DigitalAsset> findById(Long id);

    DigitalAsset save(DigitalAsset asset);

    DigitalAssetModel create(CreateDigitalAssetCommand command);

    DecryptTokenModel decrypt(DecryptDigitalAssetCommand command);

    DecryptedDigitalAssetModel consumeSecretToken(ConsumeSecretTokenCommand command);

    boolean canDecryptAssetShare(AssetShare assetShare);

    void softDeleteAsset(Long assetId);
}
