package com.example.reminder.repository;

import com.example.reminder.entity.DigitalAssetSecretToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DigitalAssetSecretTokenRepository extends JpaRepository<DigitalAssetSecretToken, Long> {

    Optional<DigitalAssetSecretToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from DigitalAssetSecretToken token
        where token.digitalAsset.id = :assetId
          and token.consumedAt is null
        """)
    int deleteActiveTokensByAssetId(@Param("assetId") Long assetId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update DigitalAssetSecretToken token
        set token.consumedAt = :consumedAt
        where token.tokenHash = :tokenHash
          and token.consumedAt is null
          and token.expiresAt > :consumedAt
          and token.actorId = :actorId
          and token.ipAddress = :ipAddress
        """)
    int markConsumedIfValid(
            @Param("tokenHash") String tokenHash,
            @Param("actorId") String actorId,
            @Param("ipAddress") String ipAddress,
            @Param("consumedAt") LocalDateTime consumedAt
    );
}
