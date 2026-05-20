package com.example.reminder.service;

import com.example.reminder.domain.model.DigitalAssetModel;
import com.example.reminder.domain.model.DecryptTokenModel;
import com.example.reminder.domain.model.DecryptedDigitalAssetModel;
import com.example.reminder.dto.digitalasset.CreateDigitalAssetCommand;
import com.example.reminder.dto.digitalasset.ConsumeSecretTokenCommand;
import com.example.reminder.dto.digitalasset.DecryptDigitalAssetCommand;
import com.example.reminder.dto.digitalasset.AssetAuditContext;
import com.example.reminder.dto.digitalasset.DigitalAssetDetailResponseDto;
import com.example.reminder.dto.digitalasset.DigitalAssetListResponseDto;
import com.example.reminder.dto.digitalasset.UpdateDigitalAssetRequest;
import com.example.reminder.entity.AssetShare;
import com.example.reminder.entity.DigitalAsset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DigitalAssetService {

    List<DigitalAsset> findByUserId(Long userId);

    Optional<DigitalAsset> findById(Long id);

    DigitalAsset save(DigitalAsset asset);

    DigitalAssetModel create(CreateDigitalAssetCommand command, AssetAuditContext auditContext);

    Page<DigitalAssetListResponseDto> getAssets(Long userId, String search, Pageable pageable);

    DigitalAssetDetailResponseDto getAsset(Long userId, Long assetId);

        DigitalAssetDetailResponseDto updateAsset(
            Long userId,
            Long assetId,
            UpdateDigitalAssetRequest request,
            AssetAuditContext auditContext
        );

        DigitalAssetDetailResponseDto updateSecret(
            Long userId,
            Long assetId,
            String secret,
            AssetAuditContext auditContext
        );

        void deleteAsset(Long userId, Long assetId, AssetAuditContext auditContext);

    DecryptTokenModel decrypt(DecryptDigitalAssetCommand command);

    DecryptedDigitalAssetModel consumeSecretToken(ConsumeSecretTokenCommand command);

    boolean canDecryptAssetShare(AssetShare assetShare);

    void softDeleteAsset(Long assetId);
}
