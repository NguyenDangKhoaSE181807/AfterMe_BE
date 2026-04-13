package com.example.reminder.repository;

import com.example.reminder.entity.DigitalAsset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigitalAssetRepository extends JpaRepository<DigitalAsset, Long> {

    List<DigitalAsset> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<DigitalAsset> findByIdAndDeletedAtIsNull(Long id);
}
