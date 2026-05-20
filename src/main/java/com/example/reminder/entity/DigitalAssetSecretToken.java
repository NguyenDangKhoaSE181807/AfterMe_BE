package com.example.reminder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "digital_asset_secret_tokens")
public class DigitalAssetSecretToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "digital_asset_id", nullable = false)
    private DigitalAsset digitalAsset;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
