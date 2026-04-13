package com.example.reminder.repository;

import com.example.reminder.entity.AssetShare;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetShareRepository extends JpaRepository<AssetShare, Long> {

    List<AssetShare> findByDigitalAssetIdAndDeletedAtIsNull(Long digitalAssetId);

    @Modifying
    @Query("""
        update AssetShare s
        set s.deletedAt = current_timestamp,
            s.unlockStatus = 'EXPIRED',
            s.isUnlocked = false
        where s.digitalAsset.id = :digitalAssetId
          and s.deletedAt is null
        """)
    int revokeAllActiveSharesByDigitalAssetId(@Param("digitalAssetId") Long digitalAssetId);
}
