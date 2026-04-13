package com.example.reminder.repository;

import com.example.reminder.entity.AssetShare;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetShareRepository extends JpaRepository<AssetShare, Long> {

    List<AssetShare> findByDigitalAssetIdAndDeletedAtIsNull(Long digitalAssetId);
}
