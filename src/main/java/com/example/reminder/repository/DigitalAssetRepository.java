package com.example.reminder.repository;

import com.example.reminder.entity.DigitalAsset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DigitalAssetRepository extends JpaRepository<DigitalAsset, Long> {

    List<DigitalAsset> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<DigitalAsset> findByIdAndDeletedAtIsNull(Long id);

        Optional<DigitalAsset> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

        @Query("""
                select asset
                from DigitalAsset asset
                where asset.user.id = :userId
                    and asset.deletedAt is null
                    and (
                        :search is null
                        or lower(asset.name) like lower(concat('%', :search, '%'))
                        or lower(asset.identifier) like lower(concat('%', :search, '%'))
                        or lower(asset.type) like lower(concat('%', :search, '%'))
                    )
                """)
        Page<DigitalAsset> searchActiveByUserId(
                        @Param("userId") Long userId,
                        @Param("search") String search,
                        Pageable pageable
        );
}
